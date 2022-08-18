package software.amazon.cloudwatchlogs.emf.logger;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.cloudwatchlogs.emf.serializers.UnitDeserializer;
import software.amazon.cloudwatchlogs.emf.serializers.UnitSerializer;
import software.amazon.cloudwatchlogs.emf.sinks.GroupedSinkShunt;
import software.amazon.cloudwatchlogs.emf.sinks.SinkShunt;

public class MetricsLoggerThreadSafetyTest {
    private volatile MetricsLogger logger;
    private EnvironmentProvider envProvider;
    private SinkShunt sink;
    private Environment environment;
    private volatile Throwable throwable = null;

    @Before
    public void setUp() {
        envProvider = mock(EnvironmentProvider.class);
        environment = mock(Environment.class);
        sink = new SinkShunt();

        when(envProvider.resolveEnvironment())
                .thenReturn(CompletableFuture.completedFuture(environment));
        when(environment.getSink()).thenReturn(sink);
        logger = new MetricsLogger(envProvider);
    }

    @Test
    public void testConcurrentPutProperty() throws InterruptedException {
        logger = new MetricsLogger(envProvider);
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
                                        int propertyId = 1000 * id + j;
                                        logger.putProperty(
                                                "Property-" + propertyId,
                                                String.valueOf(propertyId));
                                    }
                                } catch (Throwable e) {
                                    throwable = e; // ensure no exceptions are thrown
                                }
                            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        logger.flush();
        for (int i = 0; i < 100000; i++) {
            Assert.assertEquals(sink.getContext().getProperty("Property-" + i), String.valueOf(i));
        }
    }

    @Test
    public void testConcurrentPutDimension() throws InterruptedException {
        logger = new MetricsLogger(envProvider);
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
                                        int dimensionId = 1000 * id + j;
                                        logger.putDimensions(
                                                DimensionSet.of(
                                                        "Dim", String.valueOf(dimensionId)));
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

        logger.flush();

        List<DimensionSet> dimensions = sink.getContext().getDimensions();
        // check size
        Assert.assertEquals(sink.getContext().getDimensions().size(), 100000);
        for (DimensionSet dim : dimensions) {
            Assert.assertEquals(dim.getDimensionKeys().size(), 4); // there are 3 default dimensions
        }
        // check content
        Collections.sort(
                dimensions,
                Comparator.comparingInt(d -> Integer.parseInt(d.getDimensionValue("Dim"))));
        for (int i = 0; i < 100000; i++) {
            Assert.assertEquals(dimensions.get(i).getDimensionValue("Dim"), String.valueOf(i));
        }
    }

    @Test
    public void testConcurrentPutDimensionAfterSetDimension() throws InterruptedException {
        logger = new MetricsLogger(envProvider);
        logger.setDimensions(DimensionSet.of("Dim", "0"));
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    for (int j = 0; j < 1000; j++) {
                                        int dimensionId = 1000 * id + j + 1;
                                        logger.putDimensions(
                                                DimensionSet.of(
                                                        "Dim", String.valueOf(dimensionId)));
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

        logger.flush();

        List<DimensionSet> dimensions = sink.getContext().getDimensions();
        // check size
        Assert.assertEquals(sink.getContext().getDimensions().size(), 100001);
        for (DimensionSet dim : dimensions) {
            Assert.assertEquals(
                    dim.getDimensionKeys().size(), 1); // there are no default dimensions after set
        }
        // check content
        Collections.sort(
                dimensions,
                Comparator.comparingInt(d -> Integer.parseInt(d.getDimensionValue("Dim"))));
        for (int i = 0; i < 100001; i++) {
            Assert.assertEquals(dimensions.get(i).getDimensionValue("Dim"), String.valueOf(i));
        }
    }

    @Test
    public void testConcurrentFlush() throws InterruptedException, JsonProcessingException {
        GroupedSinkShunt groupedSink = new GroupedSinkShunt();
        when(envProvider.resolveEnvironment())
                .thenReturn(CompletableFuture.completedFuture(environment));
        when(environment.getSink()).thenReturn(groupedSink);

        logger = new MetricsLogger(envProvider);
        Thread[] threads = new Thread[300];
        long targetTimestampToRun = System.currentTimeMillis() + 1000;

        for (int i = 0; i < 300; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    // try to putMetric() and flush() at the same time
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    logger.putMetric("Metric-" + id, id);
                                    logger.flush();
                                } catch (Throwable e) {
                                    throwable = e;
                                }
                            });
            threads[i].start();
        }

        for (Thread t : threads) {
            t.join();
        }

        ArrayList<MetricDefinitionCopy> allMetrics = new ArrayList<>();
        for (List<String> events : groupedSink.getLogEventList()) {
            ArrayList<MetricDefinitionCopy> metrics = parseAllMetrics(events);
            allMetrics.addAll(metrics);
        }

        assertEquals(allMetrics.size(), 300);
        for (MetricDefinitionCopy metric : allMetrics) {
            assertEquals(metric.getValues().size(), 1);
        }
        Collections.sort(allMetrics, Comparator.comparingDouble(m -> m.getValues().get(0)));
        for (int i = 0; i < 300; i++) {
            assertEquals(allMetrics.get(i).getName(), "Metric-" + i);
            assertEquals(allMetrics.get(i).getValues().get(0), i, 1e-5);
        }
    }

    @Test
    public void testConcurrentFlushAndPutMetric()
            throws InterruptedException, JsonProcessingException {
        GroupedSinkShunt groupedSink = new GroupedSinkShunt();
        when(envProvider.resolveEnvironment())
                .thenReturn(CompletableFuture.completedFuture(environment));
        when(environment.getSink()).thenReturn(groupedSink);

        logger = new MetricsLogger(envProvider);
        Random rand = new Random();

        Thread[] threads = new Thread[500];
        for (int i = 0; i < 500; i++) {
            final int id = i;
            int randTime = rand.nextInt(1000);
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    // half threads do putMetric(), half do flush()
                                    // sleep to introduce more chaos in thread ordering
                                    Thread.sleep(randTime);
                                    if (id % 2 == 0) {
                                        for (int j = id * 500; j < id * 500 + 1000; j++) {
                                            logger.putMetric("Metric-" + j, j);
                                        }
                                    } else {
                                        logger.flush();
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
        logger.flush();

        ArrayList<MetricDefinitionCopy> allMetrics = new ArrayList<>();
        for (List<String> events : groupedSink.getLogEventList()) {
            ArrayList<MetricDefinitionCopy> metrics = parseAllMetrics(events);
            allMetrics.addAll(metrics);
        }

        assertEquals(allMetrics.size(), 250000);
        for (MetricDefinitionCopy metric : allMetrics) {
            assertEquals(metric.getValues().size(), 1);
        }
        Collections.sort(allMetrics, Comparator.comparingDouble(m -> m.getValues().get(0)));
        for (int i = 0; i < 250000; i++) {
            assertEquals(allMetrics.get(i).getName(), "Metric-" + i);
            assertEquals(allMetrics.get(i).getValues().get(0), i, 1e-5);
        }
    }

    @Test
    public void testConcurrentFlushAndMethodsOtherThanPutMetric() throws InterruptedException {
        GroupedSinkShunt groupedSink = new GroupedSinkShunt();
        when(envProvider.resolveEnvironment())
                .thenReturn(CompletableFuture.completedFuture(environment));
        when(environment.getSink()).thenReturn(groupedSink);

        logger = new MetricsLogger(envProvider);
        Random rand = new Random();

        Thread[] threads = new Thread[600];
        for (int i = 0; i < 600; i++) {
            final int id = i;
            int randTime = rand.nextInt(1000);
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(randTime);
                                    if (id < 200) {
                                        for (int j = id * 100; j < id * 100 + 100; j++) {
                                            logger.putDimensions(
                                                    DimensionSet.of("Dim", String.valueOf(j)));
                                        }
                                    } else if (id < 400) {
                                        for (int k = id * 100; k < id * 100 + 100; k++) {
                                            logger.putProperty("Property-" + k, k);
                                        }
                                    } else {
                                        logger.flush();
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
        logger.flush();

        int contextNum = groupedSink.getContexts().size();
        MetricsContext finalContext = groupedSink.getContexts().get(contextNum - 1);
        List<DimensionSet> dimensions = finalContext.getDimensions();

        // check dimension size
        assertEquals(dimensions.size(), 20000);
        for (DimensionSet dim : dimensions) {
            Assert.assertEquals(dim.getDimensionKeys().size(), 4); // there are 3 default dimensions
        }
        // check dimension content
        Collections.sort(
                dimensions,
                Comparator.comparingInt(d -> Integer.parseInt(d.getDimensionValue("Dim"))));
        for (int i = 0; i < 2000; i++) {
            Assert.assertEquals(dimensions.get(i).getDimensionValue("Dim"), String.valueOf(i));
        }

        // check property
        int propertyCnt = 0;
        for (MetricsContext mc : groupedSink.getContexts()) {
            for (int i = 20000; i < 40000; i++) {
                propertyCnt += mc.getProperty("Property-" + i) == null ? 0 : 1;
            }
        }
        assertEquals(propertyCnt, 20000);
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
        throwable = null; // reset throwable to prevent repeat throwing
    }

    private Map<String, Object> parseRootNode(String event) throws JsonProcessingException {
        return new JsonMapper().readValue(event, new TypeReference<Map<String, Object>>() {});
    }

    @Test
    public void testParseMetrics() throws JsonProcessingException {
        for (int i = 0; i < 150; i++) {
            logger.putMetric("Metric-" + i, i);
        }
        logger.flush();

        ArrayList<MetricDefinitionCopy> metrics = parseAllMetrics(sink.getLogEvents());
        System.out.println(metrics.size());
        for (String line : sink.getLogEvents()) {
            System.out.println(line);
        }

        for (MetricDefinitionCopy metric : metrics) {
            assertEquals(metric.getValues().size(), 1);
        }
        Collections.sort(metrics, Comparator.comparingDouble(m -> m.getValues().get(0)));
        for (int i = 0; i < 150; i++) {
            assertEquals(metrics.get(i).getName(), "Metric-" + i);
            assertEquals(metrics.get(i).getValues().get(0), i, 1e-5);
        }
    }

    @SuppressWarnings("unchecked")
    // can parse all metrics even if metric number exceeds MAX_METRICS_PER_EVENT
    private ArrayList<MetricDefinitionCopy> parseAllMetrics(List<String> events)
            throws JsonProcessingException {
        ArrayList<MetricDefinitionCopy> metricDefinitions = new ArrayList<>();
        for (String event : events) {
            Map<String, Object> rootNode = parseRootNode(event);
            Map<String, Object> metadata = (Map<String, Object>) rootNode.get("_aws");

            if (metadata == null) {
                continue;
            }

            ArrayList<Map<String, Object>> metricDirectives =
                    (ArrayList<Map<String, Object>>) metadata.get("CloudWatchMetrics");
            ArrayList<Map<String, String>> metrics =
                    (ArrayList<Map<String, String>>) metricDirectives.get(0).get("Metrics");

            for (Map<String, String> metric : metrics) {
                String name = metric.get("Name");
                Unit unit = Unit.fromValue(metric.get("Unit"));
                Object value = rootNode.get(name);
                if (value instanceof ArrayList) {
                    metricDefinitions.add(new MetricDefinitionCopy(name, unit, (ArrayList) value));
                } else {
                    metricDefinitions.add(new MetricDefinitionCopy(name, unit, (double) value));
                }
            }
        }

        return metricDefinitions;
    }

    @AllArgsConstructor
    private class MetricDefinitionCopy {
        @NonNull
        @Getter
        @JsonProperty("Name")
        private String name;

        @Getter
        @JsonProperty("Unit")
        @JsonSerialize(using = UnitSerializer.class)
        @JsonDeserialize(using = UnitDeserializer.class)
        private Unit unit;

        @JsonIgnore @NonNull @Getter private List<Double> values;

        MetricDefinitionCopy(String name) {
            this(name, Unit.NONE, new ArrayList<>());
        }

        MetricDefinitionCopy(String name, double value) {
            this(name, Unit.NONE, value);
        }

        MetricDefinitionCopy(String name, Unit unit, double value) {
            this(name, unit, new ArrayList<>(Arrays.asList(value)));
        }
    }
}
