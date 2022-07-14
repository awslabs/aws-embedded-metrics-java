package software.amazon.cloudwatchlogs.emf.model;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.After;
import org.junit.Test;

public class MetricsContextThreadSafetyTest {
    private volatile Throwable throwable = null;

    @Test
    public void testConcurrentPutMetaData() throws InterruptedException {
        MetricsContext mc = new MetricsContext();
        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    mc.putMetadata("MetaData-" + id, String.valueOf(id));
                                } catch (Throwable e) {
                                    throwable = e;
                                }
                            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        Map<String, Object> metaData = mc.getRootNode().getAws().getCustomMetadata();
        assertEquals(metaData.size(), 100);
        for (int i = 0; i < 100; i++) {
            assertEquals(metaData.get("MetaData-" + i), String.valueOf(i));
        }
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
    }
}
