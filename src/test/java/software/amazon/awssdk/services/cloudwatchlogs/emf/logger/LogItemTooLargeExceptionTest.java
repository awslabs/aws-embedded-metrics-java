package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.List;

public class LogItemTooLargeExceptionTest {
    // Tests for coverage
    @Test
    public void constructorsTest() {
        String msg = "msg";
        List<EMFLogItem> logItems = null;
        Throwable cause = null;

        new LogItemTooLargeException(msg);
        new LogItemTooLargeException(msg, logItems, logItems);
        new LogItemTooLargeException(msg, cause, logItems, logItems);
        new LogItemTooLargeException(cause, logItems, logItems);
        new LogItemTooLargeException(logItems, logItems);
    }
}
