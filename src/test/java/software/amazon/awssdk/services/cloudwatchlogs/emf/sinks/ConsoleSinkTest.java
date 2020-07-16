package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Tests just for coverage
public class ConsoleSinkTest {
    /**
     * Test to get coverage
     */
    @Test
    public void testConsoleSinkConstructor() {
        new ConsoleSink();
        ConsoleSink.builder().toString();
        ConsoleSink.builder().build();
    }

    @Test
    public void testConsoleSink() throws FlushException {
        // Create a log item to serialize
        EMFLogItem logItem = EMFTestUtilities.createLargeLogItem(0);

        List<EMFLogItem> logItems = new ArrayList<>();
        logItems.add(logItem);
        ConsoleSink sink = ConsoleSink.builder().build();
        sink.accept(logItems);
    }


    @Test(expected = FlushException.class)
    public void testJsonProcessingException() throws FlushException, JsonProcessingException {
        EMFLogItem li = Mockito.spy(new EMFLogItem());
        Mockito.when(li.serializeMetrics()).thenThrow(new JsonProcessingException("Oh no!") {});

        List<EMFLogItem> logItems = Arrays.asList(li);
        ConsoleSink sink = ConsoleSink.builder().build();
        sink.accept(logItems);
    }
}
