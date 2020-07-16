package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.CloudWatchLimits;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;
import software.amazon.awssdk.services.cloudwatchlogs.model.CloudWatchLogsException;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.RejectedLogEventsInfo;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CloudWatchLogsClientSinkTestBase {
    protected final String itemsOutOfOrderMessage = "Log Items out of order";
    List<InputLogEvent> processedLogItems;

    // Helper function to change the value in a private, static field
    // These tests modify some constants to make testing faster, and more practical.
    void setFinalStatic(Field field, Object newValue) throws Exception {
        field.setAccessible(true);

        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        //modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);

        field.set(null, newValue);
    }


    Answer createCloudWatchLogsClientPugLogEventsRequestSimulation() {
        Answer ret =
                invocation -> {
                    PutLogEventsRequest req = invocation.getArgument(0);
                    long earliestTime = OffsetDateTime.now(ZoneOffset.UTC)
                            .minusDays(CloudWatchLimits.getNumDaysTooBeforeTooOld())
                            .toInstant()
                            .toEpochMilli();
                    long latestTime = OffsetDateTime.now(ZoneOffset.UTC)
                            .plusHours(CloudWatchLimits.getNumHoursBeforeTooNew())
                            .toInstant()
                            .toEpochMilli();

                    // validate the items are in date order
                    long lastTimeStamp = Long.MIN_VALUE;
                    for(InputLogEvent le : req.logEvents()) {
                        if (le.timestamp() < lastTimeStamp)
                            throw CloudWatchLogsException.builder()
                                    .message(itemsOutOfOrderMessage)
                                    .build();

                        lastTimeStamp = le.timestamp();
                    }

                    Integer tooOldIdx = null;
                    Integer tooNewIdx = null;
                    int batchSize = 0;

                    // Validate we never get more items than we expect in a batch
                    assertTrue(String.format("Number of items in batch %d exceeds limit of %d",
                            req.logEvents().size(),
                            CloudWatchLimits.getMaxBatchSizeInBytes()),
                            req.logEvents().size() <= CloudWatchLimits.getMaxLogEventsInBatch());

                    for (int i = 0; i < req.logEvents().size(); ++i) {
                        InputLogEvent le = req.logEvents().get(i);
                        batchSize += SinkUtilities.getFullEncodedLogEntryLength(le.message());

                        // Validate the total size of a batch never exceeds the limit
                        assertTrue(String.format("Batch size %d exceeds limit of %d",
                                req.logEvents().size(),
                                CloudWatchLimits.getMaxBatchSizeInBytes()),
                                batchSize <= CloudWatchLimits.getMaxBatchSizeInBytes());

                        if(le.timestamp() < earliestTime) {
                            tooOldIdx = i;
                            // Only capture the first idx for too new
                        } else if (le.timestamp() > latestTime) {
                            if (tooNewIdx == null)
                                tooNewIdx = i;
                        } else {
                            processedLogItems.add(le);
                        }
                    }
                    if (tooOldIdx != null)
                        tooOldIdx = tooOldIdx+1;

                    PutLogEventsResponse.Builder builder = PutLogEventsResponse.builder();
                    if(tooOldIdx != null || tooNewIdx != null) {
                        builder = builder.rejectedLogEventsInfo(RejectedLogEventsInfo.builder()
                                .tooOldLogEventEndIndex(tooOldIdx)
                                .tooNewLogEventStartIndex(tooNewIdx)
                                .build());
                    }

                    PutLogEventsResponse resp = builder.build();

                    return resp;
                };

        return ret;
    }


    // Create a mock CloudWatch client that will simulate the exected behavior.
    CloudWatchLogsClient createCloudWatchLogsClientMock() {
        CloudWatchLogsClient cloudWatchLogsClient = mock(CloudWatchLogsClient.class);
        when(cloudWatchLogsClient.putLogEvents(Mockito.any(PutLogEventsRequest.class)))
                .then(createCloudWatchLogsClientPugLogEventsRequestSimulation());

        when(cloudWatchLogsClient.describeLogStreams(Mockito.any(DescribeLogStreamsRequest.class)))
                .thenReturn(DescribeLogStreamsResponse.builder()
                .logStreams(LogStream.builder().uploadSequenceToken("oiuwerkadsioywkjhwer").build())
                .nextToken("lkjwerouiwlekrjwker")
                .build());

        return cloudWatchLogsClient;
    }


    // Create log items using the given logItemSupplier.
    List<EMFLogItem> createLogItems(int timeOffset, int startingId, int numItems, Instant now, Function<Integer, EMFLogItem> logItemSupplier) {
        List<EMFLogItem> logItems = new ArrayList<>(numItems);
        for(int i = 0; i < numItems; ++i) {
            EMFLogItem li =logItemSupplier.apply(startingId + i);
            li.setTimestamp(now.plusSeconds(timeOffset).plusMillis(i));
            logItems.add(li);
        }

        return logItems;
    }


    // Simplified logic to try to determine the failed, and unprocessed log items given a set of log items.
    // Assumes the log items are in order.
    public void getExpectedFailedAndUnprocessedItems(List<EMFLogItem> logItems, Set<EMFLogItem> expectedFailedLogItems, Set<EMFLogItem> expectedUnprocessedLogItems) throws JsonProcessingException {
        final int batchSizeMax = CloudWatchLimits.getMaxBatchSizeInBytes(); // Cloudwatch Logs batch size limit
        final int maxEventSize = CloudWatchLimits.getMaxEventSizeInBytes();
        final int maxLogEventsInBatch = CloudWatchLimits.getMaxLogEventsInBatch();

        int batchCount = 0;
        int batchSize = 0;
        boolean anyFailures = false;
        boolean markRestUnprocessed = false;

        long earliestTime = OffsetDateTime.now(ZoneOffset.UTC)
                .minusDays(CloudWatchLimits.getNumDaysTooBeforeTooOld())
                .toInstant()
                .toEpochMilli();

        long latestTime = OffsetDateTime.now(ZoneOffset.UTC)
                .plusHours(CloudWatchLimits.getNumHoursBeforeTooNew())
                .toInstant()
                .toEpochMilli();


        int count = 0;
        int startOfBatchIdx = 0;
        for (EMFLogItem li : logItems) {
            int itemSize = SinkUtilities.getFullEncodedLogEntryLength(li.serialize());
            batchSize += itemSize;

            if (batchCount >= maxLogEventsInBatch) {
                if (anyFailures) {
                    markRestUnprocessed = true;
                }
                startOfBatchIdx = count;
                // Starting a new batch with this item, set batchSize to this item
                // And batch count to 1
                batchSize = itemSize;
                batchCount = 0;
            }

            if (itemSize > maxEventSize && !markRestUnprocessed) {
                // Special case where a single item is too large to send by itself
                // These checks will happen before actually talking to CW, so clear everything, and start over.
                expectedFailedLogItems.clear();
                expectedFailedLogItems.add(li);

                expectedUnprocessedLogItems.clear();
                expectedUnprocessedLogItems.addAll(logItems.stream()
                        .skip(startOfBatchIdx)
                        .filter(i -> !i.equals(li))
                        .collect(Collectors.toSet()));

                return;
            }

            if (batchSize > batchSizeMax) {
                if (anyFailures) {
                    markRestUnprocessed = true;
                }
                startOfBatchIdx = count;
                // Starting a new batch with this item, set batchSize to this item
                // And batch count to 1
                batchSize = itemSize;
                batchCount = 0;
            }

            ++batchCount;

            if (!markRestUnprocessed) {
                if (li.getTimestamp().toEpochMilli() < earliestTime) {
                    anyFailures = true;
                    expectedFailedLogItems.add(li);
                } else if (li.getTimestamp().toEpochMilli() > latestTime) {
                    anyFailures = true;
                    expectedFailedLogItems.add(li);
                } else {
                    // Log items that passed
                }
            } else {
                // Had a failure, out of this batch, just fail everything else.
                expectedUnprocessedLogItems.add(li);
            }

            ++count;
        }
    }


    // Match 2 lists, to make sure they contain the same items.
    public void matchLists(String message, Collection<EMFLogItem> expectedItems, Collection<EMFLogItem> actualItems) {
        assertEquals(expectedItems.size(), actualItems.size());
        for (EMFLogItem li : actualItems) {
            assertTrue(message, expectedItems.contains(li));
        }

        // Verify the reverse is true
        for (EMFLogItem li : expectedItems) {
            assertTrue(message, actualItems.contains(li));
        }
    }


    // Run after flushing logItems to sink to make sure we got the actual and expected numbers of processed, failed,
    // and unprocessed log items match.
    public void checkFailedUnprocessedAndProcssedLogItems(List<EMFLogItem> logItems, List<EMFLogItem> failedLogItems, List<EMFLogItem> unprocessedLogItems) throws JsonProcessingException {
        Set<EMFLogItem> expectedFailedLogItems = new HashSet<>();
        Set<EMFLogItem> expectedUnprocessedLogItems = new HashSet<>();

        getExpectedFailedAndUnprocessedItems(logItems, expectedFailedLogItems, expectedUnprocessedLogItems);

        // Verify all failedLogItems match expectedFailedLogItems
        matchLists("Mismatch in failed items", expectedFailedLogItems, failedLogItems);

        // Verify all unprocessedLogItems
        matchLists("Mismatch in unprocessed items", expectedUnprocessedLogItems, unprocessedLogItems);

        // Verify processedLogItems is the subset of logItems with failedLogItems and unprocessedLogItems removed
        List<EMFLogItem> expectedProcessedLogItems =
                logItems.stream()
                        .filter(i -> !expectedFailedLogItems.contains(i))
                        .filter(i -> !unprocessedLogItems.contains(i))
                        .collect(Collectors.toList());
        assertEquals(expectedProcessedLogItems.size(), processedLogItems.size());

        // The log items should be in order, and the timestamps should be unique.
        // we're comparing EMFLogItems to InputLogEvents here.
        for (int i = 0; i < expectedProcessedLogItems.size(); ++i) {
            EMFLogItem li = expectedProcessedLogItems.get(i);
            InputLogEvent le = processedLogItems.get(i);
            assertEquals((long)li.getTimestamp().toEpochMilli(), (long)le.timestamp());
        }
    }


    // Run a single test with the given number of too old items, in time range items, and too new items.
    public void testTooOldNormalAndTooNew(int numTooOldItems,
                                          int numNormalItems,
                                          int numTooNewItems,
                                          boolean exceptionExpected,
                                          Function<Integer, EMFLogItem> logItemSupplier)
            throws JsonProcessingException {
        // clear the processedLogItems
        processedLogItems = new LinkedList<>();

        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();
        CloudWatchLogsClientSink cwSink = CloudWatchLogsClientSink.builder()
            .client(cloudWatchLogsClient)
            .logGroup("aLogGroup")
            .logStream("aLogStream")
            .build();

        List<EMFLogItem> logItems = new ArrayList<>(numTooOldItems + numNormalItems + numTooNewItems);

        Instant now = Instant.now();

        int timeOffset = numTooOldItems*2;

        // Create some items that are more than double getNumDaysTooBeforeTooOld days old to account for test runtime
        List<EMFLogItem> tmpLogItems = createLogItems(
                -CloudWatchLimits.getNumDaysTooBeforeTooOld()*24*60*60*2 - timeOffset,
                0,
                numTooOldItems,
                now,
                logItemSupplier);

        logItems.addAll(tmpLogItems);

        // Create some normal items that should be accepted
        tmpLogItems = createLogItems(0, numTooOldItems, numNormalItems, now, logItemSupplier);
        logItems.addAll(tmpLogItems);

        // Create some items that are more than double getNumHoursBeforeTooNew hours in the future
        tmpLogItems = createLogItems(
                 // 2 hours into the future * 2 to account for test run time.
                CloudWatchLimits.getNumHoursBeforeTooNew()*60*60*2,
                numTooOldItems+numNormalItems,
                numTooNewItems,
                now,
                logItemSupplier);

        logItems.addAll(tmpLogItems);

        boolean exceptionTaken = false;
        List<EMFLogItem> failedLogItems = new ArrayList<>();
        List<EMFLogItem> unprocessedLogItems = new ArrayList<>();
        try {
            cwSink.accept(logItems);
        } catch (FlushException e) {
            exceptionTaken = true;
            failedLogItems = e.getFailedLogItems();
            unprocessedLogItems = e.getUnprocessedLogItems();
        }
        assertEquals(exceptionExpected, exceptionTaken);
        checkFailedUnprocessedAndProcssedLogItems(logItems, failedLogItems, unprocessedLogItems);
    }



    // Run a series of tests with permutations of too old, in time range, and too new items covering
    // some specific corner cases, then some random cases.
    public void testTooOldNormalTooNewPermutations(Function<Integer, EMFLogItem> logItemSupplier)
            throws Exception {

        final int maxNumItemsInBatch = CloudWatchLimits.getMaxLogEventsInBatch();

        final int numItemsMin = maxNumItemsInBatch * 10; // 10 batches minimum
        final int numItemsMax = numItemsMin * 2;

        int numTooOldItems;
        int numNormalItems;
        int numTooNewItems;



        // Too old alone
        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                0,
                0,
                true,
                logItemSupplier);



        // normal alone
        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(0,
                numNormalItems,
                0,
                false,
                logItemSupplier);



        // too new alone
        numTooNewItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(0,
                0,
                numTooNewItems,
                true,
                logItemSupplier);



        // Too old and normal
        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(1,
                numNormalItems,
                0,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                1,
                0,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                numNormalItems,
                0,
                true,
                logItemSupplier);



        // Too old and normal and too new
        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(1,
                numNormalItems,
                1,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                1,
                1,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                numNormalItems,
                1,
                true,
                logItemSupplier);

        numTooNewItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(1,
                1,
                numTooNewItems,
                true,
                logItemSupplier);

        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(1,
                numNormalItems,
                numTooNewItems,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                1,
                numTooNewItems,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                numNormalItems,
                numTooNewItems,
                true,
                logItemSupplier);



        // normal and too new
        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(0,
                numNormalItems,
                1,
                true,
                logItemSupplier);


        numNormalItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(0,
                numNormalItems,
                numTooNewItems,
                true,
                logItemSupplier);




        // Too old  and too new
        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                0,
                1,
                true,
                logItemSupplier);

        numTooNewItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(1,
                0,
                numTooNewItems,
                true,
                logItemSupplier);

        numTooOldItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        numTooNewItems = EMFTestUtilities.randInt(numItemsMin, numItemsMax);
        testTooOldNormalAndTooNew(numTooOldItems,
                0,
                numTooNewItems,
                true,
                logItemSupplier);
    }
}
