package software.amazon.cloudwatchlogs.emf;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.sinks.SinkShunt;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 3, time = 5)
@Measurement(iterations = 3, time = 5)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Fork(value = 1)
public class MetricsLoggerBenchmark {
    private MetricsLogger logger;
    private EnvironmentProvider envProvider;
    private SinkShunt sink;
    private Environment environment;

    @Setup
    public void setUp() {
        envProvider = mock(EnvironmentProvider.class);
        environment = mock(Environment.class);
        sink = new SinkShunt();

        when(envProvider.resolveEnvironment())
                .thenReturn(CompletableFuture.completedFuture(environment));
        when(environment.getSink()).thenReturn(sink);
        logger = new MetricsLogger(envProvider);
    }

    /**
     * Publishing 10000 metrics with single thread.
     * no lock: 0.844 ms/op; RW lock: 0.896 ms/op; S lock: 0.884 ms/op
     */
    @Benchmark
    public void measurePutMetric() {
        logger = new MetricsLogger(envProvider); // 0.024 ms/op

        // should make this op dominate running time
        for (int i = 0; i < 10000; i++) {
            logger.putMetric("Metric-" + i, i);
        }
    }

    /**
     * Flush with single thread.
     * no lock: 0.148 ms/op; RW lock: 0.148 ms/op; S lock: 0.147 ms/op
     */
    @Benchmark
    public void measureFlush() {
        logger = new MetricsLogger(envProvider);

        logger.flush();

        sink.shutdown();
    }

    /**
     * Invoke all methods 100 times with single thread.
     * no lock: 6.946 ms/op; RW lock: 6.988 ms/op; S lock: 6.823 ms/op
     */
    @Benchmark
    public void measureAllMethods() {
        logger = new MetricsLogger(envProvider);

        for (int j = 0; j < 100; j++) {
            logger.putMetadata("MetaData-" + j, j);
            logger.putProperty("Property-" + j, j);
            logger.putDimensions(DimensionSet.of("Dim-" + j, String.valueOf(j)));
            logger.putMetric("Metric-" + j, j);
            logger.flush();
        }

        sink.shutdown();
    }

    /**
     * Publish 10000 metrics with 5 threads.
     * no lock: 0.758 ms/op; RW lock: 2.816 ms/op; S lock: 2.247 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith5Threads() throws InterruptedException {
        measurePutMetricWithNThreads(5);
    }

    /**
     * Publish 10000 metrics with 10 threads.
     * no lock: 0.949 ms/op; RW lock: 3.823 ms/op; S lock: 3.078 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith10Threads() throws InterruptedException {
        measurePutMetricWithNThreads(10);
    }

    /**
     * Publish 10000 metrics with 20 threads.
     * no lock: 1.610 ms/op; RW lock: 3.349 ms/op; S lock: 2.644 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith20Threads() throws InterruptedException {
        measurePutMetricWithNThreads(20);
    }

    /**
     * Publish 10000 metrics with 50 threads.
     * no lock: 4.161 ms/op; RW lock: 4.107 ms/op; S lock: 4.184 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith50Threads() throws InterruptedException {
        measurePutMetricWithNThreads(50);
    }

    /**
     * Publish 10000 metrics with 100 threads.
     * no lock: 8.648 ms/op; RW lock: 9.071 ms/op; S lock: 8.576 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith100Threads() throws InterruptedException {
        measurePutMetricWithNThreads(100);
    }

    /**
     * Flush 1000 times with 5 threads.
     * no lock: 7.529 ms/op; RW lock: 22.742 ms/op; S lock: 23.304 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith5Threads() throws InterruptedException {
        measureFlushWithNThreads(5);
    }

    /**
     * Flush 1000 times with 10 threads.
     * no lock: 12.900 ms/op; RW lock: 25.015 ms/op; S lock: 24.778 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith10Threads() throws InterruptedException {
        measureFlushWithNThreads(10);
    }

    /**
     * Flush 1000 times with 20 threads.
     * no lock: 6.537 ms/op; RW lock: 25.705 ms/op; S lock: 26.465 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith20Threads() throws InterruptedException {
        measureFlushWithNThreads(20);
    }

    /**
     * Flush 1000 times with 50 threads.
     * no lock: 24.985 ms/op; RW lock: 31.453 ms/op; S lock: 31.965 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith50Threads() throws InterruptedException {
        measureFlushWithNThreads(50);
    }

    /**
     * Flush 1000 times with 100 threads.
     * no lock: 34.527 ms/op; RW lock: 39.606 ms/op; S lock: 40.007 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith100Threads() throws InterruptedException {
        measureFlushWithNThreads(100);
    }

    /**
     * Flush 1000 times with 1000 threads.
     * no lock: 116.047 ms/op; RW lock: 141.227 ms/op; S lock: 141.597 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 10)
    @Measurement(time = 10)
    public void measureFlushWith1000Threads() throws InterruptedException {
        measureFlushWithNThreads(1000);
    }

    /**
     * Execute all methods for 1000 times with 5 threads.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective):
     * 84.041 ± 24.965 ms/op;
     * RW lock: 264.439 ± 8.070 ms/op; S lock: 264.630 ± 24.252 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measureAllMethodsWith5Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(5);
    }

    /**
     * Execute all methods for 1000 times with 10 threads.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective):
     * 41.174 ± 6.084 ms/op;
     * RW lock: 263.103 ± 15.141 ms/op; S lock: 256.267 ± 30.922 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    public void measureAllMethodsWith10Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(10);
    }

    /**
     * Execute all methods for 1000 times with 100 threads.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective):
     * 35.779 ± 2.414 ms/op;
     * RW lock: 315.340 ± 16.074 ms/op; S lock: 288.459 ± 5.801 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 10)
    @Measurement(time = 10)
    public void measureAllMethodsWith100Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(100);
    }

    /**
     * Execute all methods for 1000 times with 500 threads.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective):
     * 81.785 ± 11.616 ms/op;
     * RW lock: 346.697 ± 51.133 ms/op; S lock: 368.981 ± 161.049 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 10)
    @Measurement(time = 10)
    public void measureAllMethodsWith500Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(500);
    }

    /**
     * Execute all methods for 1000 times with 1000 threads.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective):
     * 218.505 ± 178.808 ms/op;
     * RW lock: 436.380 ± 317.130 ms/op; S lock: 390.074 ± 100 ms/op
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 10)
    @Measurement(time = 10)
    public void measureAllMethodsWith1000Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(1000);
    }

    private void measurePutMetricWithNThreads(int n) throws InterruptedException {
        logger = new MetricsLogger(envProvider);
        Thread[] threads = new Thread[n];

        for (int i = 0; i < n; i++) {
            final int id = i;
            int batchSize = 10000 / n;
            threads[i] =
                    new Thread(
                            () -> {
                                for (int j = batchSize * id; j < batchSize * id + batchSize; j++) {
                                    logger.putMetric("Metric-" + j, j);
                                }
                            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }
    }

    private void measureFlushWithNThreads(int n) throws InterruptedException {
        logger = new MetricsLogger(envProvider);
        Thread[] threads = new Thread[n];

        for (int i = 0; i < n; i++) {
            final int id = i;
            int batchSize = 1000 / n;
            threads[i] =
                    new Thread(
                            () -> {
                                for (int j = batchSize * id; j < batchSize * id + batchSize; j++) {
                                    logger.flush();
                                }
                            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        sink.shutdown();
    }

    private void measureAllMethodsWithNThreads(int n) throws InterruptedException {
        logger = new MetricsLogger(envProvider);
        Thread[] threads = new Thread[n];

        for (int i = 0; i < n; i++) {
            final int id = i;
            int batchSize = 1000 / n;
            threads[i] =
                    new Thread(
                            () -> {
                                for (int j = batchSize * id; j < batchSize * id + batchSize; j++) {
                                    logger.putMetadata("MetaData-" + id, id);
                                    logger.putProperty("Property-" + id, id);
                                    logger.putDimensions(
                                            DimensionSet.of("Dim-" + id, String.valueOf(id)));
                                    logger.putMetric("Metric-" + j, j);
                                    logger.flush();
                                }
                            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        sink.shutdown();
    }
}
