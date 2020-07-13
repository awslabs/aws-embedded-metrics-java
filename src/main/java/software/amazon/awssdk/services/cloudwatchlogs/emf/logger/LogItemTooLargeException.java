package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.List;

/**
 * EMFLogger Flush exception thrown when a single EMFLogItem is too large to send on its own.
 */
public class LogItemTooLargeException extends FlushException {
    public LogItemTooLargeException(
            String message,
            Throwable cause,
            List<EMFLogItem> failedLogItems,
            List<EMFLogItem> unprocessedLogItems) {
        super(message, cause, failedLogItems, unprocessedLogItems);
    }

    public LogItemTooLargeException(
            String message,
            List<EMFLogItem> failedLogItems,
            List<EMFLogItem> unprocessedLogItems) {
        super(message, failedLogItems, unprocessedLogItems);
    }

    public LogItemTooLargeException(
            Throwable cause,
            List<EMFLogItem> failedLogItems,
            List<EMFLogItem> unprocessedLogItems) {
        super(cause, failedLogItems, unprocessedLogItems);
    }

    public LogItemTooLargeException(
            List<EMFLogItem> failedLogItems,
            List<EMFLogItem> unprocessedLogItems) {
        super(failedLogItems, unprocessedLogItems);
    }

    // Internal constructor for handling errors before all information is available
    public LogItemTooLargeException(String message) {
        super(message);
    }
}
