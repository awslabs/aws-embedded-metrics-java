package software.amazon.awssdk.services.cloudwatchlogs.emf;

import com.fasterxml.jackson.core.JsonProcessingException;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.CloudWatchLimits;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.EMFLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks.SinkUtilities;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.CloudwatchMetricCollection;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Try log items of various sizes around the limits, to make sure we don't get any unexpected CloudWatch exceptions.
 */
public class LogItemSizeTest extends IntegrationTestBase {

    public static void main(String[] args) throws FlushException, JsonProcessingException, InterruptedException {
        CloudWatchLogsClient logsClient = createCloudWatchLogsClient();
        EMFLogger logger = createEMFLogger(logsClient);

        testTooLargeItem(logger, logsClient);
        testAtLimitItem(logger, logsClient);

        System.out.printf("Done!\n");

        System.exit(0);
    }

    // Test a log item that is just over the limit
    public static void testTooLargeItem(EMFLogger logger, CloudWatchLogsClient logsClient)
            throws JsonProcessingException {
        System.out.printf("Running: testTooLargeItem\n");
        final int maxEventSize = CloudWatchLimits.getMaxEventSizeInBytes();
        // Test a log item that is just over the limit
        EMFLogItem logItem = logger.createLogItem();
        CloudwatchMetricCollection metricsCollection = logItem.createMetricsCollection();
        metricsCollection.putDimension(dimensionName, dimensionValue);
        metricsCollection.setNamespace(namespace);
        metricsCollection.putMetric(metricName, 1);

        String uniqueId = UUID.randomUUID().toString();
        metricsCollection.putProperty("uniqueId", uniqueId);

        int logItemSize = SinkUtilities.getFullEncodedLogEntryLength(logItem.serialize() + System.lineSeparator());

        int remainingSize = maxEventSize - logItemSize;

        // Test too big
        String rawLogMessage = getRandomString(remainingSize+1);
        int size = rawLogMessage.getBytes(StandardCharsets.UTF_8).length;
        logItem.setRawLogMessage(rawLogMessage);
        boolean tookException = false;
        try {
            logger.flush();
        } catch (FlushException e) {
            tookException = true;
            return;
        }

        System.out.printf("testTooLargeItems: expected an exception, but none thrown\n");
        System.exit(-1);
    }


    // Test a log item that is right at the limit
    public static void testAtLimitItem(EMFLogger logger, CloudWatchLogsClient logsClient)
            throws JsonProcessingException, InterruptedException {
        System.out.printf("Running: testAtLimitItem\n");
        final int maxEventSize = CloudWatchLimits.getMaxEventSizeInBytes();
        // Test a log item that is just over the limit
        EMFLogItem logItem = logger.createLogItem();
        CloudwatchMetricCollection metricsCollection = logItem.createMetricsCollection();
        metricsCollection.putDimension(dimensionName, dimensionValue);
        metricsCollection.setNamespace(namespace);
        metricsCollection.putMetric(metricName, 1);

        String uniqueId = UUID.randomUUID().toString();
        metricsCollection.putProperty("uniqueId", uniqueId);

        int logItemSize = SinkUtilities.getFullEncodedLogEntryLength(logItem.serialize() + System.lineSeparator());

        int remainingSize = maxEventSize - logItemSize;

        Instant startTime = Instant.now();

        // Test too big
        String rawLogMessage = getRandomString(remainingSize);
        int size = rawLogMessage.getBytes(StandardCharsets.UTF_8).length;
        logItem.setRawLogMessage(rawLogMessage);
        boolean tookException = false;
        try {
            logger.flush();
        } catch (FlushException e) {
            System.out.printf("testAtLimitItem: expected no exception, but one thrown\n");
            System.exit(-1);
        }

        System.out.printf("testAtLimitItem: Sleeping\n");
        Thread.sleep(10*1000);
        System.out.printf("testAtLimitItem: Checking if log made it to CloudWatch\n");
        // item must have sent, verify it's actually in CW
        GetLogEventsRequest getLogEventsRequest = GetLogEventsRequest.builder()
                .logGroupName(logGroup)
                .logStreamName(logStream)
                .startTime(startTime.minusSeconds(60).toEpochMilli())
                .limit(100)
                .build();

        GetLogEventsResponse logEventsResp = logsClient.getLogEvents(getLogEventsRequest);
        for (OutputLogEvent ole : logEventsResp.events()) {
            if (ole.message().contains(uniqueId))
                return; // success
        }

        System.out.printf("testAtLimitItem: Couldn't find message that matched what was sent to CloudWatch\n");
        System.exit(-1);
    }

}
