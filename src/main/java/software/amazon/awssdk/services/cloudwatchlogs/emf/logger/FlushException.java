package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.cloudwatchlogs.emf.EMFException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.List;

/**
 * Exception thrown when flushing logItems to CloudWatch fails for any reason.
 */
@Getter
@Setter
public class FlushException extends EMFException {
    /**
     * The list of logItems that failed to flush.
     */
    private List<EMFLogItem> failedLogItems = null;


    /**
     * The list of logItems that were never operated on due to preceding errors.
     */
    private List<EMFLogItem> unprocessedLogItems = null;

    public FlushException(
            String message,
            Throwable cause,
            List<EMFLogItem> failedLogItems,
            List<EMFLogItem> unprocessedLogItems) {
        super(message, cause);
        this.failedLogItems = failedLogItems;
        this.unprocessedLogItems = unprocessedLogItems;
    }

    public FlushException(String message, List<EMFLogItem> failedLogItems, List<EMFLogItem> unprocessedLogItems) {
        super(message);
        this.failedLogItems = failedLogItems;
        this.unprocessedLogItems = unprocessedLogItems;
    }

    public FlushException(Throwable cause, List<EMFLogItem> failedLogItems, List<EMFLogItem> unprocessedLogItems) {
        super(cause);
        this.failedLogItems = failedLogItems;
        this.unprocessedLogItems = unprocessedLogItems;
    }

    public FlushException(List<EMFLogItem> failedLogItems, List<EMFLogItem> unprocessedLogItems) {
        this.failedLogItems = failedLogItems;
        this.unprocessedLogItems = unprocessedLogItems;
    }

    public FlushException(String message, Throwable cause) {
        super(message, cause);
    }

    // Internal constructor for handling errors before all information is available
    protected FlushException(String message) {
        super(message);
    }
}
