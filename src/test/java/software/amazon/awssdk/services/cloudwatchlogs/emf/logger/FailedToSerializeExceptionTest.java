package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.List;

public class FailedToSerializeExceptionTest {
    // Tests for coverage
    @Test
    public void constructorTest() {
        String msg = "msg";
        List<EMFLogItem> logItems = null;
        Throwable cause = null;

        new FailedToSerializeException(msg);
        new FailedToSerializeException(msg, logItems, logItems);
        new FailedToSerializeException(msg, cause, logItems, logItems);
        new FailedToSerializeException(cause, logItems, logItems);
        new FailedToSerializeException(logItems, logItems);
    }
}
