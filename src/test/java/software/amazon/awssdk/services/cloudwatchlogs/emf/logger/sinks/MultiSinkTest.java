package software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks;

import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertTrue;
import static software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities.checkThrows;

// Test just for coverage
public class MultiSinkTest {

    @Test
    public void testMultiSinkConstructor() {
        ConsoleSink sink = ConsoleSink.builder().build();
        List<ISink> sinks = Arrays.asList(sink);

        MultiSink.builder().toString();
        MultiSink.builder().clearSinks();

        assertTrue(checkThrows(
                () -> {
                    new MultiSink(null);
                },
                NullPointerException.class
        ));

        // This doesn't throw because Lombok doesn't require the param here not be null.
        MultiSink.builder().sink(null).build();

        assertTrue(checkThrows(
                () -> {
                    MultiSink.builder().sinks(null).build();
                },
                NullPointerException.class
        ));

        MultiSink.builder().build();
        MultiSink.builder().sinks(sinks).build();

        MultiSink.builder().sinks(sinks).build();
        MultiSink.builder().sinks(sinks).sinks(sinks).build();
        MultiSink.builder().sinks(sinks).clearSinks().build();

        MultiSink.builder().sink(sink).build();
    }

    @Test
    public void testMultiSink() throws FlushException {

        // Create a log item to serialize
        EMFLogItem logItem = EMFTestUtilities.createLargeLogItem(0);

        List<EMFLogItem> logItems = new ArrayList<>();
        logItems.add(logItem);

        MultiSink sink = MultiSink.builder()
                .sink(ConsoleSink.builder().build())
                .sink(ConsoleSink.builder().build())
                .build();

        sink.accept(logItems);
    }
}
