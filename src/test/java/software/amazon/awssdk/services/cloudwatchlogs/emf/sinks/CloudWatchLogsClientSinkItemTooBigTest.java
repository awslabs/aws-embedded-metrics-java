package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.CloudWatchLimits;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.LogItemTooLargeException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test cases where a log item is bigger than the max batch size, making it impossible to send.
 */
public class CloudWatchLogsClientSinkItemTooBigTest extends CloudWatchLogsClientSinkTestBase {
    int maxNumItemsInBatchSaved;
    int maxEventSizeSaved;

    public void setup(int maxLogEventsInBatch, int maxEventSize) throws Exception {
        // Change the limits in the CloudWatchLogsClientSink
        maxNumItemsInBatchSaved = CloudWatchLimits.getMaxLogEventsInBatch();
        maxEventSizeSaved = CloudWatchLimits.getMaxEventSizeInBytes();

        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxLogEventsInBatch"), maxLogEventsInBatch);
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxEventSizeInBytes"), maxEventSize);
    }

    public void teardown() throws Exception {
        // Change them back to normal
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxLogEventsInBatch"), maxNumItemsInBatchSaved);
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxEventSizeInBytes"), maxEventSizeSaved);
    }


    public EMFLogItem createTooBigItem(int id) {
        int maxBatchSize = CloudWatchLimits.getMaxBatchSizeInBytes();

        EMFLogItem logItem = EMFTestUtilities.createTinyLogItem(id);
        MetricsContext metricsContext =logItem.createMetricsContext();
        // Assume int is 4 bytes
        // Add more than enough properties to metric to make it too big.
        for (Integer i = 0; i < maxBatchSize / 4; ++i) {
            metricsContext.putProperty(i.toString(), i.toString());
        }

        return logItem;
    }


    public void testTooBigItem(int maxLogEventsInBatch, int maxEventSize) throws Exception {
        try {
            setup(maxLogEventsInBatch, maxEventSize);

            CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();
            CloudWatchLogsClientSink cwSink = CloudWatchLogsClientSink.builder()
                    .client(cloudWatchLogsClient)
                    .logGroup("aLogGroup")
                    .logStream("aLogStream")
                    .build();

            // Set processedLogItems in the base class.
            processedLogItems = new LinkedList<>();

            assertEquals(maxLogEventsInBatch, CloudWatchLimits.getMaxLogEventsInBatch());
            assertEquals(maxEventSize, CloudWatchLimits.getMaxEventSizeInBytes());
            int numLogEvents = EMFTestUtilities.randInt(maxLogEventsInBatch * 3, maxLogEventsInBatch * 4);
            int idxOfLargeLogEvent = EMFTestUtilities.randInt(maxLogEventsInBatch + 1, numLogEvents - 1);

            List<EMFLogItem> logItems = new ArrayList<>(numLogEvents);
            EMFLogItem tooLargeLogItem = null;

            for (int i = 0; i < numLogEvents; ++i) {
                EMFLogItem logItem;
                if (i == idxOfLargeLogEvent) {
                    logItem = createTooBigItem(i);
                    tooLargeLogItem = logItem;
                } else {
                    logItem = EMFTestUtilities.createTinyLogItem(i);
                }

                logItems.add(logItem);
            }

            boolean tookException = false;
            try {
                cwSink.accept(logItems);
            } catch (FlushException e) {
                tookException = true;
                assertTrue(e instanceof LogItemTooLargeException);
                assertTrue(e.getFailedLogItems().size() == 1);
                assertTrue(e.getFailedLogItems().get(0).equals(tooLargeLogItem));

                checkFailedUnprocessedAndProcssedLogItems(logItems, e.getFailedLogItems(), e.getUnprocessedLogItems());
            }

            assertTrue(tookException);
        } finally {
            teardown();
        }
    }


    /**
     * Tests the common case with a large batch size
     * @throws Exception
     */
    @Test
    public void testTooBigItem100Batch() throws Exception {
        testTooBigItem(100, 64*1024);
    }


    /**
     * Test the corner case with batch size of 1, this should force unprocessed item count to 0
     * @throws Exception
     */
    @Test
    public void testTooBigItem1Batch() throws Exception {
        testTooBigItem(1, 64*1024);
    }

}
