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
    private volatile Throwable throwable;

    @Before
    public void setUp() {
        envProvider = mock(EnvironmentProvider.class);
        environment = mock(Environment.class);
        sink = new SinkShunt();
        throwable = null;

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
                                        logger.putProperty("Property-" + propertyId, String.valueOf(propertyId));
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
                                        logger.putDimensions(DimensionSet.of("Dim", String.valueOf(dimensionId)));
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
                                        logger.putDimensions(DimensionSet.of("Dim", String.valueOf(dimensionId)));
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
        Thread[] threads = new Thread[100];
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        for (int i = 0; i < 100; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
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
            ArrayList<MetricDefinitionCopy> metrics = parseMetrics(events);
            allMetrics.addAll(metrics);
        }

        assertEquals(allMetrics.size(), 100);
        for (MetricDefinitionCopy metric : allMetrics) {
            assertEquals(metric.getValues().size(), 1);
        }
        Collections.sort(allMetrics, Comparator.comparingDouble(m -> m.getValues().get(0)));
        for (int i = 0; i < 100; i++) {
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
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    // half threads do putMetric(), half do flush()
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    if (id % 2 == 0) {
                                        for (int j = id; j < id + 2; j++) {
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
            ArrayList<MetricDefinitionCopy> metrics = parseMetrics(events);
            allMetrics.addAll(metrics);
        }

        assertEquals(allMetrics.size(), 100);
        for (MetricDefinitionCopy metric : allMetrics) {
            assertEquals(metric.getValues().size(), 1);
        }
        Collections.sort(allMetrics, Comparator.comparingDouble(m -> m.getValues().get(0)));
        for (int i = 0; i < 100; i++) {
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
//        Random rand = new Random();
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        Thread[] threads = new Thread[100];
        for (int i = 0; i < 100; i++) {
            final int id = i;
//            int randTime = rand.nextInt(1000);
            threads[i] =
                    new Thread(
                            () -> {
                                try {
//                                    Thread.sleep(randTime);
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    if (id < 30) {
                                        logger.putDimensions(
                                                DimensionSet.of("Dim", String.valueOf(id)));
                                    } else if (id < 40) {
                                        logger.putProperty("Property-" + id, id);
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
        // check size
        assertEquals(dimensions.size(), 30);
        for (DimensionSet dim : dimensions) {
            Assert.assertEquals(dim.getDimensionKeys().size(), 4); // there are 3 default dimensions
        }
        // check content
        Collections.sort(
                dimensions,
                Comparator.comparingInt(d -> Integer.parseInt(d.getDimensionValue("Dim"))));
        for (int i = 0; i < 30; i++) {
            Assert.assertEquals(dimensions.get(i).getDimensionValue("Dim"), String.valueOf(i));
        }

        int propertyCnt = 0;
        for (MetricsContext mc : groupedSink.getContexts()) {
            propertyCnt += mc.getProperty("Property-30") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-31") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-32") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-33") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-34") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-35") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-36") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-37") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-38") == null ? 0 : 1;
            propertyCnt += mc.getProperty("Property-39") == null ? 0 : 1;
        }
        assertEquals(propertyCnt, 10);
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
    }

    private Map<String, Object> parseRootNode(String event) throws JsonProcessingException {
        return new JsonMapper().readValue(event, new TypeReference<Map<String, Object>>() {});
    }

    @SuppressWarnings("unchecked")
    // can not parse all metrics if metric number exceeds MAX_METRICS_PER_EVENT
    private ArrayList<MetricDefinitionCopy> parseMetrics(List<String> events)
            throws JsonProcessingException {
        Map<String, Object> rootNode = parseRootNode(events.get(0));
        Map<String, Object> metadata = (Map<String, Object>) rootNode.get("_aws");

        if (metadata == null) {
            return new ArrayList<>();
        }

        ArrayList<Map<String, Object>> metricDirectives =
                (ArrayList<Map<String, Object>>) metadata.get("CloudWatchMetrics");
        ArrayList<Map<String, String>> metrics =
                (ArrayList<Map<String, String>>) metricDirectives.get(0).get("Metrics");

        ArrayList<MetricDefinitionCopy> metricDefinitions = new ArrayList<>();
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
