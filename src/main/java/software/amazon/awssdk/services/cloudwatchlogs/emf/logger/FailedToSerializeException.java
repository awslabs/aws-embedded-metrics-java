package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.List;

/**
 * EMFLogger Flush exception.  A log item failed to serialize properly.
 */
public class FailedToSerializeException extends FlushException {
    public FailedToSerializeException(
            String message,
            Throwable cause,
            List<EMFLogItem> failedLogItems,
            List<EMFLogItem> unprocessedLogItems) {
        super(message, cause, failedLogItems, unprocessedLogItems);
    }

    public FailedToSerializeException(
            String message,
            List<EMFLogItem> failedLogItems,
            List<EMFLogItem> unprocessedLogItems) {
        super(message, failedLogItems, unprocessedLogItems);
    }

    public FailedToSerializeException(
            Throwable cause,
            List<EMFLogItem> failedLogItems,
            List<EMFLogItem> unprocessedLogItems) {
        super(cause, failedLogItems, unprocessedLogItems);
    }

    public FailedToSerializeException(List<EMFLogItem> failedLogItems, List<EMFLogItem> unprocessedLogItems) {
        super(failedLogItems, unprocessedLogItems);
    }

    // Internal constructor for handling errors before all information is available.
    protected FailedToSerializeException(String message) {
        super(message);
    }
}
