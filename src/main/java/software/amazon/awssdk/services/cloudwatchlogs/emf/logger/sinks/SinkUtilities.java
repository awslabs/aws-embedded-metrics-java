package software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks;

import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.CloudWatchLimits;

import java.nio.charset.StandardCharsets;

/**
 * Utility functions that might be useful between sink implementations.
 */
public class SinkUtilities {

    protected SinkUtilities(){}

    /**
     * Get the UTF-8 encoded bytes from the logEntry.
     * @param logEntry
     * @return
     */
    public static byte[] getEncodedLogEntry(String logEntry) {
        return logEntry.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Get the length of the logEntry after encoding it to UTF-8.
     * @param logEntry
     * @return
     */
    public static int getEncodedLogEntryLength(String logEntry) {
        return getEncodedLogEntry(logEntry).length;
    }

    /**
     * Get the actual size of the message that will be sent to CloudWatch for this log entry.
     * @param logEntry
     * @return
     */
    public static int getFullEncodedLogEntryLength(String logEntry) {
        return getEncodedLogEntry(logEntry).length + CloudWatchLimits.getExtraSizePerMessageInBytes();
    }
}
