package software.amazon.cloudwatchlogs.emf.model;

import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.junit.After;
import org.junit.Test;

public class MetricsContextThreadSafetyTest {
    private volatile Throwable throwable = null;

    @Test
    public void testConcurrentPutMetaData() throws InterruptedException {
        final int N_THREAD = 100;
        final int N_PUT_METADATA = 1000;

        MetricsContext mc = new MetricsContext();
        Thread[] threads = new Thread[N_THREAD];
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        for (int i = 0; i < N_THREAD; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    for (int j = 0; j < N_PUT_METADATA; j++) {
                                        int metaDataId = N_PUT_METADATA * id + j;
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
        assertEquals(metaData.size(), N_THREAD * N_PUT_METADATA);
        for (int i = 0; i < N_THREAD * N_PUT_METADATA; i++) {
            assertEquals(metaData.get("MetaData-" + i), i);
        }
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
        throwable = null;
    }
}
