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
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        for (int i = 0; i < 100; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    for (int j = 0; j < 1000; j++) {
                                        int metaDataId = 1000 * id + j;
                                        mc.putMetadata("MetaData-" + metaDataId, metaDataId);
                                    }
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
        assertEquals(metaData.size(), 100000);
        for (int i = 0; i < 100000; i++) {
            assertEquals(metaData.get("MetaData-" + i), i);
        }
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
        throwable = null;
    }
}
