package software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.CloudWatchLimits;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;

import java.util.function.Function;

public class CloudWatchLogsClientSinkSizeBatchingTest extends CloudWatchLogsClientSinkTestBase {
    int maxNumItemsInBatchSaved;
    int maxBatchSizeSaved;

    @Before
    public void setup() throws Exception {
        // Change the limits in the CloudWatchLogsClientSink
        maxNumItemsInBatchSaved = CloudWatchLimits.getMaxLogEventsInBatch();
        maxBatchSizeSaved = CloudWatchLimits.getMaxBatchSizeInBytes();
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxLogEventsInBatch"), 100);
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxBatchSizeInBytes"), 2048);
    }

    @After
    public void teardown() throws Exception {
        // Change them back to normal
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxLogEventsInBatch"), maxNumItemsInBatchSaved);
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxBatchSizeInBytes"), maxBatchSizeSaved);
    }

    @Test
    public void testTooOldNormalTooNewCombinationsSizeBatching() throws Exception {
        // Change the limits in the CloudWatchLogsClientSink

        Function<Integer, EMFLogItem> largeLogItemSupplier = id -> EMFTestUtilities.createLargeLogItem(id);
        testTooOldNormalTooNewPermutations(largeLogItemSupplier);

    }
}
