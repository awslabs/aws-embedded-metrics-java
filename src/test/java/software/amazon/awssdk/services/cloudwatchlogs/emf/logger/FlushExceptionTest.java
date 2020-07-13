package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.List;

public class FlushExceptionTest {

    // Tests for coverage
    @Test
    public void constructorsTest() {
        String msg = "msg";
        List<EMFLogItem> logItems = null;
        Throwable cause = null;

        new FlushException(msg);
        new FlushException(msg, logItems, logItems);
        new FlushException(msg, cause, logItems, logItems);
        new FlushException(cause, logItems, logItems);
        new FlushException(logItems, logItems);
        new FlushException(msg).setFailedLogItems(logItems);
        new FlushException(msg).setUnprocessedLogItems(logItems);
    }
}
