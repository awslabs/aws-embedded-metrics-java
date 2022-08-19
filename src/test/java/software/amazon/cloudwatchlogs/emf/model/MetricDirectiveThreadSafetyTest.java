package software.amazon.cloudwatchlogs.emf.model;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import org.junit.After;
import org.junit.Test;

public class MetricDirectiveThreadSafetyTest {
    private volatile Throwable throwable = null;

    @Test
    public void testConcurrentPutMetricWithDifferentKey() throws InterruptedException {
        final int N_THREAD = 100;
        final int N_PUT_METRIC = 1000;

        MetricDirective metricDirective = new MetricDirective();
        Thread[] threads = new Thread[N_THREAD];
        long targetTimestampToRun =
                System.currentTimeMillis()
                        + 500; // all threads should target running on this timestamp

        for (int i = 0; i < N_THREAD; i++) {
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
                                    for (int j = 0; j < N_PUT_METRIC; j++) {
                                        int metricId = N_PUT_METRIC * id + j;
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

        assertEquals(metricDirective.getAllMetrics().size(), N_THREAD * N_PUT_METRIC);
        for (int i = 0; i < N_THREAD * N_PUT_METRIC; i++) {
            assertEquals(
                    metricDirective.getMetrics().get("Metric-" + i).getValues().get(0), i, 1e-5);
        }
    }

    @Test
    public void testConcurrentPutMetricWithSameKey() throws InterruptedException {
        final int N_THREAD = 100;
        final int N_PUT_METRIC = 1000;

        MetricDirective metricDirective = new MetricDirective();
        Thread[] threads = new Thread[N_THREAD];
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        for (int i = 0; i < N_THREAD; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    for (int j = 0; j < N_PUT_METRIC; j++) {
                                        int metricId = N_PUT_METRIC * id + j;
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
        for (int i = 0; i < N_THREAD * N_PUT_METRIC; i++) {
            assertEquals(md.getValues().get(i), i, 1e-5);
        }
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
        throwable = null;
    }
}
