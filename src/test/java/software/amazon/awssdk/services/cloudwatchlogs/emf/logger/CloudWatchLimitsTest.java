package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import org.junit.Test;

public class CloudWatchLimitsTest {
    @Test
    public void getLimitsTest() {
        CloudWatchLimits limits = new CloudWatchLimits();
        CloudWatchLimits.getExtraSizePerMessageInBytes();
        CloudWatchLimits.getMaxBatchSizeInBytes();
        CloudWatchLimits.getMaxEventSizeInBytes();
        CloudWatchLimits.getMaxLogEventsInBatch();
        CloudWatchLimits.getNumDaysTooBeforeTooOld();
        CloudWatchLimits.getNumHoursBeforeTooNew();
    }
}
