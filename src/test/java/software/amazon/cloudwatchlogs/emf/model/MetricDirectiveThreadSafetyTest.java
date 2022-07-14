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
        for (int i = 0; i < 100; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    metricDirective.putMetric("Metric-" + id, id);
                                } catch (Throwable e) {
                                    throwable = e;
                                }
                            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertEquals(metricDirective.getAllMetrics().size(), 100);
        for (int i = 0; i < 100; i++) {
            assertEquals(
                    metricDirective.getMetrics().get("Metric-" + i).getValues().get(0), i, 1e-5);
        }
    }

    @Test
    public void testConcurrentPutMetricWithSameKey() throws InterruptedException {
        MetricDirective metricDirective = new MetricDirective();
        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    metricDirective.putMetric("Metric", id);
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
        for (int i = 0; i < 100; i++) {
            assertEquals(md.getValues().get(i), i, 1e-5);
        }
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
    }
}
