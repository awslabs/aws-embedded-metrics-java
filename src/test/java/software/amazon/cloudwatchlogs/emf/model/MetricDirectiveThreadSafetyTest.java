package software.amazon.cloudwatchlogs.emf.model;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.After;
import org.junit.Test;

public class MetricDirectiveThreadSafetyTest {
    private volatile Throwable throwable = null;

    @Test
    public void testConcurrentPutMetricWithDifferentKey() throws InterruptedException {
        MetricDirective metricDirective = new MetricDirective();
        Thread[] threads = new Thread[100];
        long targetTimestampToRun =
                System.currentTimeMillis()
                        + 500; // all threads should target running on this timestamp

        for (int i = 0; i < 100; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(
                                            targetTimestampToRun
                                                    - System.currentTimeMillis()); // try to make
                                    // all threads
                                    // run at same
                                    // time
                                    for (int j = 0; j < 1000; j++) {
                                        int metricId = 1000 * id + j;
                                        metricDirective.putMetric("Metric-" + metricId, metricId);
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

        assertEquals(metricDirective.getAllMetrics().size(), 100000);
        for (int i = 0; i < 100000; i++) {
            assertEquals(
                    metricDirective.getMetrics().get("Metric-" + i).getValues().get(0), i, 1e-5);
        }
    }

    @Test
    public void testConcurrentPutMetricWithSameKey() throws InterruptedException {
        MetricDirective metricDirective = new MetricDirective();
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
                                        int metricId = 1000 * id + j;
                                        metricDirective.putMetric("Metric", metricId);
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

        assertEquals(metricDirective.getAllMetrics().size(), 1);
        MetricDefinition md = metricDirective.getAllMetrics().toArray(new MetricDefinition[0])[0];
        Collections.sort(md.getValues());
        for (int i = 0; i < 100000; i++) {
            assertEquals(md.getValues().get(i), i, 1e-5);
        }
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
        throwable = null;
    }
}
