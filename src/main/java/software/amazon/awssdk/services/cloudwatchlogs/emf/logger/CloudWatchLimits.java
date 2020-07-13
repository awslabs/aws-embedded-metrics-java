package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import lombok.Getter;

/**
 * Constants for CloudWatch Logs limits.
 * These limits can be found here:
 * https://docs.aws.amazon.com/AmazonCloudWatchLogs/latest/APIReference/API_PutLogEvents.html
 * and here:
 * https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/cloudwatch_limits_cwl.html
 */
public class CloudWatchLimits {
    /**
     * Empty constructor
     */
    protected CloudWatchLimits() {}

    // Java treats finals differently than normal static items.
    // Tests use reflection to modify these, even though they're private.
    // Setting these as final will break tests.  Do not set to final.

    /** Max size in bytes of a single batch of log items CloudWatch will accept */
    @Getter
    private static int maxBatchSizeInBytes = 1024 * 1024;

    /** Max size of a single log event */
    @Getter
    private static int maxEventSizeInBytes = 256 * 1024;

    /** Number of additional bytes the CloudWatchLogsClient adds to each log item */
    @Getter
    private static int extraSizePerMessageInBytes = 26;

    /** The maximum number of log items CloudWatch will accept in a batch */
    @Getter
    private static int maxLogEventsInBatch = 10000;

    /** How many days older than the current time a log item can be before it is rejected */
    @Getter
    private static int numDaysTooBeforeTooOld = 14;

    /** How many hours ahead of the current time a log item can be before it is rejected */
    @Getter
    private static int numHoursBeforeTooNew = 2;
}
