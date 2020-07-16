package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import org.junit.Test;
import org.mockito.Mockito;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.MultiSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.TestSink;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doThrow;
import static software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities.checkThrows;

public class EMFLoggerTest {

    /**
     * Test for coverage
     */
    @Test
    public void testEMFLoggerConstructor() {
        ConsoleSink sink = ConsoleSink.builder().build();

        new EMFLogger(sink);
        EMFLogger.builder().logSink(sink).build();

        boolean exceptionTaken = false;
        assertTrue(checkThrows(
                () -> new EMFLogger(null),
                NullPointerException.class
        ));


        assertTrue(checkThrows(
                () -> EMFLogger.builder().logSink(null).build(),
                NullPointerException.class
        ));

        EMFLogger.builder().toString();
    }

    @Test
    public void testEMFLogger() throws FlushException {
        TestSink testSink = new TestSink();
        ConsoleSink consoleSink = ConsoleSink.builder().build();
        EMFLogger logger = EMFLogger.builder()
                    .logSink(MultiSink.builder()
                        .sink(testSink)
                        .sink(consoleSink)
                        .build()
                    )
                .build();
        logger.enableJsonPrettyPrinting(true);

        final int numItems = EMFTestUtilities.randInt(1, 100);
        List<EMFLogItem> logItems = new ArrayList<>(numItems);

        for(int i = 0; i < numItems; ++i) {
            EMFLogItem logItem = logger.createLogItem();
            EMFTestUtilities.createLargeLogItem(logItem, i);
            logItems.add(logItem);
        }

        logger.flush();

        // Compare the log items sent to flush to the log we created
        assertEquals(logItems.size(), testSink.getSeenLogItems().size());
        for(EMFLogItem li : testSink.getSeenLogItems()) {
            List<EMFLogItem> collect = logItems.stream()
                    .filter(i -> i.equals(li))
                    .collect(Collectors.toList());
            assertEquals(1, collect.size());
        }

        assertEquals(0, logger.getLogItems().size());

        // test addLogItems
        testSink.getSeenLogItems().clear();
        logger.addLogItems(logItems);
        logger.flush();

        assertEquals(logItems.size(), testSink.getSeenLogItems().size());
        for(EMFLogItem li : testSink.getSeenLogItems()) {
            List<EMFLogItem> collect = logItems.stream()
                    .filter(i -> i.equals(li))
                    .collect(Collectors.toList());
            assertEquals(1, collect.size());
        }

        logger.enableJsonPrettyPrinting(false);
    }

    @Test(expected = FlushException.class)
    public void testEMFLoggerWithFailedLogItems() throws FlushException {
        final int numItems = EMFTestUtilities.randInt(1, 100);
        List<EMFLogItem> logItems = new ArrayList<>(numItems);

        TestSink testSink = Mockito.spy(new TestSink());
        doThrow(new FlushException("Failed to flush!")).when(testSink).accept(logItems);

        EMFLogger logger = EMFLogger.builder()
                .logSink(testSink)
                .build();


        for (int i = 0; i < numItems; ++i) {
            EMFLogItem logItem = logger.createLogItem();
            EMFTestUtilities.createLargeLogItem(logItem, i);
            logItems.add(logItem);
        }

        logger.flush();
        assertEquals(0, logger.getLogItems().size());
    }
}
