package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.CloudWatchLimits;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;

import java.util.function.Function;

public class CloudWatchLogsClientSinkNumItemsBatchingTest extends CloudWatchLogsClientSinkTestBase {
    int maxNumItemsInBatchSaved;

    @Before
    public void setup() throws Exception {
        // Change the limits in the CloudWatchLogsClientSink
        maxNumItemsInBatchSaved = CloudWatchLimits.getMaxLogEventsInBatch();
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxLogEventsInBatch"), 100);
    }

    @After
    public void teardown() throws Exception {
        // Change them back to normal
        setFinalStatic(CloudWatchLimits.class.getDeclaredField("maxLogEventsInBatch"), maxNumItemsInBatchSaved);
    }

    @Test
    public void testTooOldNormalTooNewCombinationsNumItemsBatching() throws Exception {
        Function<Integer, EMFLogItem> largeLogItemSupplier = id -> EMFTestUtilities.createTinyLogItem(id);
        testTooOldNormalTooNewPermutations(largeLogItemSupplier);

    }
}
