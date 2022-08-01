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
     * Publishing 10000 metrics with single thread. no lock: 0.844 ms/op; RW lock: 0.896 ms/op; S
     * lock: 0.884 ms/op
     */
    @Benchmark
    public void measurePutMetric() {
        logger = new MetricsLogger(envProvider); // 0.024 ms/op

        // should make this op dominate running time
        for (int i = 0; i < 10000; i++) {
            logger.putMetric("Metric-" + i, i);
        }
    }

    /** Flush with single thread. no lock: 0.148 ms/op; RW lock: 0.148 ms/op; S lock: 0.147 ms/op */
    @Benchmark
    public void measureFlush() {
        logger = new MetricsLogger(envProvider);

        logger.flush();

        sink.shutdown();
    }

    /**
     * Invoke all methods 100 times with single thread. no lock: 6.946 ms/op; RW lock: 6.988 ms/op;
     * S lock: 6.823 ms/op
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
     * Each thread publishes 1000 metrics, 10 threads in total.
     * no lock: 0.949 ms/op; RW lock: 3.823 ms/op; S lock: 3.078 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith10Threads() throws InterruptedException {
        measurePutMetricWithNThreads(10);
    }

    /**
     * Each thread publishes 1000 metrics, 20 threads in total.
     * no lock: 1.860 ms/op; RW lock: 9.806 ms/op; S lock: 7.929 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith20Threads() throws InterruptedException {
        measurePutMetricWithNThreads(20);
    }

    /**
     * Each thread publishes 1000 metrics, 50 threads in total.
     * no lock: 6.548 ms/op; RW lock: 28.754 ms/op; S lock: 24.700 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith50Threads() throws InterruptedException {
        measurePutMetricWithNThreads(50);
    }

    /**
     * Each thread publishes 1000 metrics, 200 threads in total.
     * no lock: 37.662 ms/op; RW lock: 135.824 ms/op; S lock: 114.467 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measurePutMetricWith200Threads() throws InterruptedException {
        measurePutMetricWithNThreads(200);
    }

    /**
     * Each thread publishes 1000 metrics, 500 threads in total.
     * no lock: 90.148 ms/op; RW lock: 345.197 ms/op; S lock: 287.908 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 10)
    @Measurement(time = 10)
    public void measurePutMetricWith500Threads() throws InterruptedException {
        measurePutMetricWithNThreads(500);
    }

    /**
     * Each thread flushes 100 times, 10 threads in total.
     * no lock: 12.900 ms/op; RW lock: 25.015 ms/op; S lock: 24.778 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith10Threads() throws InterruptedException {
        measureFlushWithNThreads(10);
    }

    /**
     * Each thread flushes 100 times, 20 threads in total.
     * no lock: 20.824 ms/op; RW lock: 47.123 ms/op; S lock: 48.511 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith20Threads() throws InterruptedException {
        measureFlushWithNThreads(20);
    }

    /**
     * Each thread flushes 100 times, 50 threads in total.
     * no lock: 77.463 ms/op; RW lock: 121.857 ms/op; S lock: 125.212 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith50Threads() throws InterruptedException {
        measureFlushWithNThreads(50);
    }

    /**
     * Each thread flushes 100 times, 200 threads in total.
     * no lock: 390.252 ms/op; RW lock: 474.439 ms/op; S lock: 488.809 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measureFlushWith200Threads() throws InterruptedException {
        measureFlushWithNThreads(200);
    }

    /**
     * Each thread flushes 100 times, 500 threads in total.
     * no lock: 300.280 ms/op; RW lock: 1161.098 ms/op; S lock: 1247.972 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 10)
    @Measurement(time = 10)
    public void measureFlushWith500Threads() throws InterruptedException {
        measureFlushWithNThreads(500);
    }

    /**
     * Each thread executes all methods 100 times, 10 threads in total.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective): 7.215 ms/op;
     * RW lock: 32.159; S lock: 34.226
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measureAllMethodsWith10Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(10);
    }

    /**
     * Each thread executes all methods 100 times, 20 threads in total.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective): 11.833 ms/op;
     * RW lock: 60.510 ms/op; S lock: 75.125 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measureAllMethodsWith20Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(20);
    }

    /**
     * Each thread executes all methods 100 times, 50 threads in total.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective): 36.051 ms/op;
     * RW lock: 150.022 ms/op; S lock: 244.934 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    public void measureAllMethodsWith50Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(50);
    }

    /**
     * Each thread executes all methods 100 times, 200 threads in total.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective): 108.775 ms/op;
     * RW lock: 629.826 ms/op; S lock: 1220.959 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 10)
    @Measurement(time = 10)
    public void measureAllMethodsWith200Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(200);
    }

    /**
     * Each thread executes all methods 100 times, 500 threads in total.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective): 335.183 ms/op;
     * RW lock: 1741.003 ms/op; S lock: 4192.327 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 10)
    @Measurement(time = 10)
    public void measureAllMethodsWith500Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(500);
    }

    /**
     * Each thread executes all methods 100 times, 1000 threads in total.
     * no lock (need to sync getAllDimensions() & getAllDimensionKeys() in MetricsDirective): 575.339 ms/op;
     * RW lock: 3230.403 ms/op; S lock: 13519.459 ms/op
     *
     * @throws InterruptedException
     */
    @Benchmark
    @Warmup(time = 20)
    @Measurement(time = 20)
    public void measureAllMethodsWith1000Threads() throws InterruptedException {
        measureAllMethodsWithNThreads(1000);
    }

    private void measurePutMetricWithNThreads(int n) throws InterruptedException {
        logger = new MetricsLogger(envProvider);
        Thread[] threads = new Thread[n];
        int batchSize = 1000;

        for (int i = 0; i < n; i++) {
            final int id = i;
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
        int batchSize = 100;

        for (int i = 0; i < n; i++) {
            final int id = i;
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
        int batchSize = 100;

        for (int i = 0; i < n; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                for (int j = batchSize * id; j < batchSize * id + batchSize; j++) {
                                    logger.putMetric("Metric-" + j, j);
                                    logger.putProperty("Property-" + j, j);
                                    logger.putMetadata("MetaData-" + j, j);
                                    logger.setDimensions(
                                            DimensionSet.of("Dim-" + j, String.valueOf(j)));

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
