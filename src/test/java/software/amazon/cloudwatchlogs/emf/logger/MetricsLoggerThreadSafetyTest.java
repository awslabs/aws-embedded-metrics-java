/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

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
        when(environment.getLogGroupName()).thenReturn("test-log-group");
        when(environment.getName()).thenReturn("test-env-name");
        when(environment.getType()).thenReturn("test-env-type");

        logger = new MetricsLogger(envProvider);
    }

    @Test
    public void testConcurrentPutProperty() throws InterruptedException {
        final int N_THREAD = 100;
        final int N_PUT_PROPERTY = 1000;

        logger = new MetricsLogger(envProvider);
        Thread[] threads = new Thread[N_THREAD];
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        for (int i = 0; i < N_THREAD; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    for (int j = 0; j < N_PUT_PROPERTY; j++) {
                                        int propertyId = N_PUT_PROPERTY * id + j;
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
        for (int i = 0; i < N_THREAD * N_PUT_PROPERTY; i++) {
            Assert.assertEquals(sink.getContext().getProperty("Property-" + i), String.valueOf(i));
        }
    }

    @Test
    public void testConcurrentPutDimension() throws InterruptedException {
        final int N_THREAD = 100;
        final int N_PUT_DIMENSIONS = 100;

        logger = new MetricsLogger(envProvider);
        // disable default dimensions
        logger.resetDimensions(false);

        Thread[] threads = new Thread[N_THREAD];
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        for (int i = 0; i < N_THREAD; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    for (int j = 0; j < N_PUT_DIMENSIONS; j++) {
                                        int dimensionId = N_PUT_DIMENSIONS * id + j;
                                        logger.putDimensions(
                                                DimensionSet.of(
                                                        String.valueOf(dimensionId),
                                                        String.valueOf(dimensionId)));
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
        Assert.assertEquals(sink.getContext().getDimensions().size(), N_THREAD * N_PUT_DIMENSIONS);
        for (DimensionSet dim : dimensions) {
            Assert.assertEquals(
                    1, dim.getDimensionKeys().size()); // default dimensions are disabled
        }
        // check content
        Collections.sort(
                dimensions,
                Comparator.comparingInt(
                        dim -> Integer.parseInt(dim.getDimensionKeys().iterator().next())));
        for (int i = 0; i < N_THREAD * N_PUT_DIMENSIONS; i++) {
            Assert.assertEquals(
                    dimensions.get(i).getDimensionValue(String.valueOf(i)), String.valueOf(i));
        }
    }

    @Test
    public void testConcurrentPutDimensionAfterSetDimension() throws InterruptedException {
        final int N_THREAD = 100;
        final int N_PUT_DIMENSIONS = 100;

        logger = new MetricsLogger(envProvider);
        logger.setDimensions(DimensionSet.of("0", "0"));
        long targetTimestampToRun = System.currentTimeMillis() + 500;

        Thread[] threads = new Thread[N_THREAD];
        for (int i = 0; i < N_THREAD; i++) {
            final int id = i;
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(targetTimestampToRun - System.currentTimeMillis());
                                    for (int j = 0; j < N_PUT_DIMENSIONS; j++) {
                                        int dimensionId = N_PUT_DIMENSIONS * id + j + 1;
                                        logger.putDimensions(
                                                DimensionSet.of(
                                                        String.valueOf(dimensionId),
                                                        String.valueOf(dimensionId)));
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
        Assert.assertEquals(
                sink.getContext().getDimensions().size(), N_THREAD * N_PUT_DIMENSIONS + 1);
        for (DimensionSet dim : dimensions) {
            Assert.assertEquals(
                    1, dim.getDimensionKeys().size()); // there are no default dimensions after set
        }
        // check content
        Collections.sort(
                dimensions,
                Comparator.comparingInt(
                        dim -> Integer.parseInt(dim.getDimensionKeys().iterator().next())));
        for (int i = 0; i < N_THREAD * N_PUT_DIMENSIONS + 1; i++) {
            Assert.assertEquals(
                    dimensions.get(i).getDimensionValue(String.valueOf(i)), String.valueOf(i));
        }
    }

    @Test
    public void testConcurrentFlush() throws InterruptedException, JsonProcessingException {
        final int N_THREAD = 300;

        GroupedSinkShunt groupedSink = new GroupedSinkShunt();
        when(envProvider.resolveEnvironment())
                .thenReturn(CompletableFuture.completedFuture(environment));
        when(environment.getSink()).thenReturn(groupedSink);

        logger = new MetricsLogger(envProvider);
        Thread[] threads = new Thread[N_THREAD];
        long targetTimestampToRun = System.currentTimeMillis() + 1000;

        for (int i = 0; i < N_THREAD; i++) {
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

        assertEquals(allMetrics.size(), N_THREAD);
        for (MetricDefinitionCopy metric : allMetrics) {
            assertEquals(1, metric.getValues().size());
        }
        Collections.sort(allMetrics, Comparator.comparingDouble(m -> m.getValues().get(0)));
        for (int i = 0; i < N_THREAD; i++) {
            assertEquals(allMetrics.get(i).getName(), "Metric-" + i);
            assertEquals(i, allMetrics.get(i).getValues().get(0), 1e-5);
        }
    }

    @Test
    public void testConcurrentFlushAndPutMetric()
            throws InterruptedException, JsonProcessingException {
        final int N_THREAD = 500;
        final int N_PUT_METRIC = 1000;

        GroupedSinkShunt groupedSink = new GroupedSinkShunt();
        when(envProvider.resolveEnvironment())
                .thenReturn(CompletableFuture.completedFuture(environment));
        when(environment.getSink()).thenReturn(groupedSink);

        logger = new MetricsLogger(envProvider);
        Random rand = new Random();

        Thread[] threads = new Thread[N_THREAD];
        for (int i = 0; i < N_THREAD; i++) {
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
                                        for (int j = id * N_PUT_METRIC / 2;
                                                j < id * N_PUT_METRIC / 2 + N_PUT_METRIC;
                                                j++) {
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

        assertEquals(allMetrics.size(), N_THREAD * N_PUT_METRIC / 2);
        for (MetricDefinitionCopy metric : allMetrics) {
            assertEquals(1, metric.getValues().size());
        }
        Collections.sort(allMetrics, Comparator.comparingDouble(m -> m.getValues().get(0)));
        for (int i = 0; i < N_THREAD * N_PUT_METRIC / 2; i++) {
            assertEquals(allMetrics.get(i).getName(), "Metric-" + i);
            assertEquals(i, allMetrics.get(i).getValues().get(0), 1e-5);
        }
    }

    @Test
    public void testConcurrentFlushAndMethodsOtherThanPutMetric() throws InterruptedException {
        final int N_THREAD = 600;
        final int N_PUT_DIMENSIONS = 100;
        final int N_PUT_PROPERTY = 100;

        GroupedSinkShunt groupedSink = new GroupedSinkShunt();
        when(envProvider.resolveEnvironment())
                .thenReturn(CompletableFuture.completedFuture(environment));
        when(environment.getSink()).thenReturn(groupedSink);

        logger = new MetricsLogger(envProvider);
        logger.resetDimensions(false);
        Random rand = new Random();

        Thread[] threads = new Thread[N_THREAD];
        for (int i = 0; i < N_THREAD; i++) {
            final int id = i;
            int randTime = rand.nextInt(1000);
            threads[i] =
                    new Thread(
                            () -> {
                                try {
                                    Thread.sleep(randTime);
                                    if (id < N_THREAD / 3) {
                                        for (int j = id * N_PUT_DIMENSIONS;
                                                j < id * N_PUT_DIMENSIONS + N_PUT_DIMENSIONS;
                                                j++) {
                                            logger.putDimensions(
                                                    DimensionSet.of(
                                                            String.valueOf(j), String.valueOf(j)));
                                        }
                                    } else if (id < N_THREAD / 3 * 2) {
                                        for (int k = id * N_PUT_PROPERTY;
                                                k < id * N_PUT_PROPERTY + N_PUT_PROPERTY;
                                                k++) {
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
        assertEquals(dimensions.size(), N_THREAD * N_PUT_DIMENSIONS / 3);
        for (DimensionSet dim : dimensions) {
            Assert.assertEquals(1, dim.getDimensionKeys().size()); // there are 3 default dimensions
        }
        // check dimension content
        Collections.sort(
                dimensions,
                Comparator.comparingInt(
                        dim -> Integer.parseInt(dim.getDimensionKeys().iterator().next())));
        for (int i = 0; i < N_THREAD * N_PUT_DIMENSIONS / 3; i++) {
            Assert.assertEquals(
                    dimensions.get(i).getDimensionValue(String.valueOf(i)), String.valueOf(i));
        }

        // check property
        int propertyCnt = 0;
        for (MetricsContext mc : groupedSink.getContexts()) {
            for (int i = N_THREAD * N_PUT_PROPERTY / 3;
                    i < N_THREAD * N_PUT_PROPERTY / 3 * 2;
                    i++) {
                propertyCnt += mc.getProperty("Property-" + i) == null ? 0 : 1;
            }
        }
        assertEquals(propertyCnt, N_THREAD * N_PUT_PROPERTY / 3);
    }

    @After
    public void tearDown() throws Throwable {
        if (throwable != null) throw throwable;
        throwable = null; // reset throwable to prevent repeat throwing
    }

    private Map<String, Object> parseRootNode(String event) throws JsonProcessingException {
        return new JsonMapper().readValue(event, new TypeReference<Map<String, Object>>() {});
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
    private static class MetricDefinitionCopy {
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
