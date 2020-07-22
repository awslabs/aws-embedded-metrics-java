package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.CloudWatchLimits;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FailedToSerializeException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.LogItemTooLargeException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogGroupRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DataAlreadyAcceptedException;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.RejectedLogEventsInfo;
import software.amazon.awssdk.services.cloudwatchlogs.model.ResourceNotFoundException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Sink to write out log items using the CloudWatchLogsClient.
 */
public class CloudWatchLogsClientSink implements ISink {
    private static Logger log = LoggerFactory.getLogger(CloudWatchLogsClientSink.class);

    private CloudWatchLogsClient logsClient;
    private String logGroup;
    private String logStream;

    private String sequenceToken = null;

    @Builder
    protected CloudWatchLogsClientSink(
            @NonNull CloudWatchLogsClient client,
            @NonNull String logGroup,
            @NonNull String logStream) {
        this.logsClient = client;
        this.logGroup = logGroup;
        this.logStream = logStream;
    }

    /**
     * Try to flush all log items using the CloudWatchLogs client.
     * Send log items in batches up to the CloudWatchLogs limits
     *
     * Any failure of any item will stop processing, and throw an exception returning all of the items that failed
     * to process.
     *
     * @param logItems
     * @throws FlushException
     */
    @Override
    public void accept(List<EMFLogItem> logItems) throws FlushException {
        // Batch log items up to 1MB at a time
        int startIdx = 0;
        int lastStartIdx = 0;
        while (startIdx < logItems.size()) {
            List<InputLogEvent> logEvents = new LinkedList<>();
            try {
                // Create a batch
                lastStartIdx = startIdx;
                startIdx = createLogsBatch(logItems, startIdx, logEvents);
            } catch (FlushException e) {
                // Unprocessed items would be everything left in logItems at startIdx or later that doesn't match
                // whatever is in failedItems.
                List<EMFLogItem> unprocessedLogItems = logItems.stream()
                        .skip(startIdx)
                        .filter(i -> !e.getFailedLogItems().contains(i))
                        .collect(Collectors.toList());
                e.setUnprocessedLogItems(unprocessedLogItems);
                throw e;
            }

            try {
                putLogEvents(logItems, logEvents, startIdx, lastStartIdx);
            } catch (SdkException e) {
                List<EMFLogItem> failedLogItems = new ArrayList<>();
                List<EMFLogItem> unprocessedLogItems = logItems.subList(lastStartIdx, logItems.size());
                throw new FlushException(
                        String.format("%s", e.getMessage()),
                        e,
                        failedLogItems,
                        unprocessedLogItems);
            }
        }
    }

    @Override
    public void accept(MetricsContext context) {
        // TODO
    }



    void putLogEvents(
            List<EMFLogItem> logItems,
            List<InputLogEvent> logEvents,
            int startIdx,
            int lastStartIdx) throws FlushException {

        // 3 retries to support unit tests.
        // Unit tests will try this call where putLogEvents fails once
        // Then createLogStream fails, then putLogEvents fails again, then
        // createLogStream succeeds, and putLogEvents succeeds.
        // There is a similar test for createLogGroup
        //
        // Also note, the way this logic works, if the logStream doesn't exist
        // we will fail once on the missing log stream, then we won't have a valid sequenceToken
        // so we'll fail once on that.  Then we have a chance at success.
        // So 3 retries are necessary for that case as well.
        final int maxRetries = 3;
        int retryCount = 0;
        PutLogEventsResponse resp = null;

        while (true) {
            SdkException thrownException = null;
            // Attempt to send the batch to CloudWatch
            PutLogEventsRequest.Builder putEventsRequestBuilder = PutLogEventsRequest.builder();
            PutLogEventsRequest putLogEventsRequest = putEventsRequestBuilder
                    .overrideConfiguration(builder ->
                            // provide the log-format header of json/emf
                            builder.headers(Collections.singletonMap(
                                    "x-amzn-logs-format",
                                    Collections.singletonList("json/emf"))))
                    .sequenceToken(sequenceToken)
                    .logEvents(logEvents)
                    .logGroupName(logGroup)
                    .logStreamName(logStream)
                    .build();

            try {
                resp = logsClient.putLogEvents(putLogEventsRequest);
                sequenceToken = resp.nextSequenceToken();
                break;
            } catch (InvalidSequenceTokenException e) {
                thrownException = e;
                sequenceToken = e.expectedSequenceToken();
            } catch (ResourceNotFoundException e) {
                thrownException = e;
                createLogGroup();
                createLogStream();
            } catch (DataAlreadyAcceptedException e) {
                // The data was already accepted, there's nothing to do here except
                // update our sequence token, and exit the loop.
                //
                // There might be a potential issue with this processing.  If the request succeeded, but the response
                // wasn't received on the final retry an SDKException would propagate up, and the logic would assume
                // nothing was processed, and thrown an excpetion back to the caller.
                //
                // If the caller changed anything in the request and retried on their own, I don't know what the
                // results would be, or if we could create an inconsistency that is irrecoverable.
                //
                // This seems like an extreme corner case, but is worth being aware of.
                sequenceToken = e.expectedSequenceToken();
                break;
            } finally {
                retryCount++;
            }

            if (retryCount == maxRetries) {
                // rethrow the exception if we've run out of retries
                throw thrownException;
            }

        }

        if (resp != null) {
            // If there is an DataAlreadyAcceptedException the data was already accepted without
            // error.  But, there will be no resp object to work with.
            // Don't know if there could be too new, too old, or expired issues that go unhandled
            // in this case, but it should be an extreme corner case at this point.
            RejectedLogEventsInfo rejectedLogEvents = resp.rejectedLogEventsInfo();
            handleRejectedLogEvents(logItems, startIdx, lastStartIdx, rejectedLogEvents);
        }
    }


    /**
     * Create the log group for our logs.
     */
    private void createLogGroup() {
        try {
            logsClient.createLogGroup(CreateLogGroupRequest.builder()
                            .logGroupName(logGroup)
                            .build());
        } catch (Exception e) {
            log.warn("Failed to create Log Group {}", logGroup);
        }
    }

    /**
     * Create the log stream for our logs.
     */
    private void createLogStream() {
        try {
            logsClient.createLogStream(CreateLogStreamRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamName(logStream)
                    .build());
        } catch (Exception e) {
            log.warn("Failed to create Log Stream {}", logStream);
        }
    }

    /**
     * Handle the case where log events are rejected for various timing reasons.
     * Sort out which logItems were sent, and which were rejected, separate into different buckets
     * to return to the caller in an exception.
     *
     * @param logItems
     * @param startIdx
     * @param lastStartIdx
     * @param rejectedLogEvents
     * @throws FlushException
     */
    void handleRejectedLogEvents(
            List<EMFLogItem> logItems,
            int startIdx,
            int lastStartIdx,
            RejectedLogEventsInfo rejectedLogEvents)
            throws FlushException {


        if (rejectedLogEvents == null) {
            // Nothing to do in this case.
            return;
        }

        // Cannot find documentation on the difference between expired log events, and too old log
        // events.
        // Assuming they must both be at the head of the array, so the max of those 2 should include all
        // of both classes.

        // Indexes in rejectedLogEvents are 0 based indexes for this batch.
        // Too old range is the start of this batch, lastStartIdx to the tooOldLogEventEndIndex() +
        // lastStartIdx.
        // If tooOldEndIdex is null the range ends up being lastStartIdx to lastStartIdx, nothing
        int tooOldStartIdxInclusive = lastStartIdx;
        int tooOldEndIdxExclusive = Math.max(
                Optional.ofNullable(rejectedLogEvents.expiredLogEventEndIndex()).orElse(0),
                Optional.ofNullable(rejectedLogEvents.tooOldLogEventEndIndex()).orElse(0))
                + lastStartIdx;

        // Too new range is tooNewLogEventStartIndex()+lastStartIdx to startIdx
        // If tooNewStartIndex is null, then the range ends up being startIdx to startIdx, nothing
        final int tmpLastStartIdx = lastStartIdx;
        int tooNewStartIdxInclusive = Optional
                .ofNullable(rejectedLogEvents.tooNewLogEventStartIndex())
                .flatMap(i -> Optional.of(i.intValue() + tmpLastStartIdx))
                .orElse(startIdx);
        int tooNewEndIdxExclusive = startIdx;

        // Once we have any failure, just give up, and return all remaining items in the list, if
        // there are any.
        int remainingStartIdx = startIdx;
        int remainingEndIdx = logItems.size();

        // failed log items will be everything before the tooOldEndIndex and after the tooNewStartIndex
        List<EMFLogItem> failedLogItems = new ArrayList<>();
        failedLogItems.addAll(logItems.subList(tooOldStartIdxInclusive, tooOldEndIdxExclusive));
        failedLogItems.addAll(logItems.subList(tooNewStartIdxInclusive, tooNewEndIdxExclusive));

        List<EMFLogItem> unprocessedLogItems = logItems.subList(remainingStartIdx, remainingEndIdx);

        throw new FlushException(
                String.format("Some log events have timestamps that are too old, or too new"),
                failedLogItems,
                unprocessedLogItems);
    }


    /**
     * Check to make sure an InputLogEvent isn't beyond any CloudWatch limits all by itself.
     * @param json
     * @throws LogItemTooLargeException
     */
    void checkLogItemTooLarge(EMFLogItem logItem, String json) throws LogItemTooLargeException {
        if (SinkUtilities.getFullEncodedLogEntryLength(json) > CloudWatchLimits.getMaxEventSizeInBytes()) {
            List<EMFLogItem> failedLogItems = Arrays.asList(logItem);
            throw new LogItemTooLargeException(
                    String.format(
                            "The maximum size of a single log item in bytes is %d submitted log item has size %d",
                            CloudWatchLimits.getMaxEventSizeInBytes()
                                    - CloudWatchLimits.getExtraSizePerMessageInBytes(),
                            SinkUtilities.getEncodedLogEntryLength(json)
                    ),
                    failedLogItems,
                    null
            );
        }
    }


    /**
     * Create a batch of InputLogEvents adhering to the CloudWatch limits
     * @param logItems list of all logItems
     * @param startIdx starting index to create the batch from
     * @param logEvents [out] a list to store the created InputLogEvents in
     * @return The new starting index
     * @throws JsonProcessingException
     */
    int createLogsBatch(List<EMFLogItem> logItems, int startIdx, List<InputLogEvent> logEvents)
            throws FlushException {
        int size = 0;
        int i;
        int count = 0;
        try {
            for (i = startIdx; i < logItems.size(); ++i) {

                // Don't include this condition in the for loop for coverage purposes.
                // Having a separate if will fail coverage if we never reach this condition in testing.
                if (count >= CloudWatchLimits.getMaxLogEventsInBatch()) {
                    break;
                }

                ++count;
                EMFLogItem logItem = logItems.get(i);
                String logEntry;
                try {
                    logEntry = logItem.serialize();
                } catch (JsonProcessingException e) {
                    List<EMFLogItem> failedLogItems = new ArrayList<>(1);
                    failedLogItems.add(logItem);

                    throw new FailedToSerializeException("Failed to serialize log entry", e, failedLogItems, null);
                }

                checkLogItemTooLarge(logItem, logEntry);

                int messageSize = SinkUtilities.getFullEncodedLogEntryLength(logEntry);
                size += messageSize;

                if (size > CloudWatchLimits.getMaxBatchSizeInBytes()) {
                    break;
                }

                InputLogEvent inputLogEvent = InputLogEvent.builder()
                        .message(logEntry)
                        .timestamp(logItem.getTimestamp().toEpochMilli())
                        .build();

                logEvents.add(inputLogEvent);
            }

            return i;
        } catch (FlushException e) {
            // Unprocessed items would be everything left in logItems at startIdx or later that doesn't match
            // whatever is in failedItems.
            List<EMFLogItem> unprocessedLogItems = logItems.stream()
                    .skip(startIdx)
                    .filter(li -> !e.getFailedLogItems().contains(li))
                    .collect(Collectors.toList());
            e.setUnprocessedLogItems(unprocessedLogItems);
            throw e;
        }
    }
}
