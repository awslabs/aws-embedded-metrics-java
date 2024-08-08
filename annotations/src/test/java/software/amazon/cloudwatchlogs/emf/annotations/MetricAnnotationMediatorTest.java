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

package software.amazon.cloudwatchlogs.emf.annotations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;

import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.cloudwatchlogs.emf.sinks.SinkShunt;

class MetricAnnotationMediatorTest {
    private MetricsLogger logger;
    private EnvironmentProvider envProvider;
    private SinkShunt sink;
    private Environment environment;
    private final Random random = new Random();

    @BeforeEach
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
        MetricAnnotationMediator.loggers.put("_defaultLogger", logger);
    }

    @Test
    void testCountMetricAnnotation()
            throws InvalidDimensionException, DimensionSetExceededException,
                    JsonProcessingException {
        for (int i = 0; i < 10; i++) {
            countMethod();
        }

        MetricAnnotationMediator.flushAll();

        for (String log : sink.getLogEvents()) {
            ArrayList<String> metricNames = parseMetricNames(log);
            Assertions.assertEquals(
                    "MetricAnnotationMediatorTest.countMethod.Count", metricNames.get(0));
            Assertions.assertEquals(
                    Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                    (ArrayList<Double>) parseMetricByName(log, metricNames.get(0)));
        }
    }

    @Test
    void testExecutionTimeMetricAnnotation() throws JsonProcessingException {
        MetricAnnotationMediator.addLogger("example logger", new MetricsLogger(envProvider));

        for (int i = 0; i < 5; i++) {
            timeMethod();
        }

        multiAnnotationMethod();
        MetricAnnotationMediator.flushAll();

        for (String log : sink.getLogEvents()) {
            ArrayList<String> metricNames = parseMetricNames(log);
            Assertions.assertEquals(
                    "MetricAnnotationMediatorTest.timeMethod.ExecutionTime", metricNames.get(0));
            ArrayList<Double> metricValues =
                    (ArrayList<Double>) parseMetricByName(log, metricNames.get(0));
            assertTrue(
                    metricValues.stream()
                            .allMatch(value -> value >= 20 && value <= 300)); // room for error
        }
    }

    @Test
    void testNamedMetricAnnotation() throws JsonProcessingException {
        MetricsLogger namedLogger = new MetricsLogger(envProvider);
        MetricAnnotationMediator.addLogger("example logger", namedLogger);

        for (int i = 0; i < 10; i++) {
            countNamedLogger();
        }

        MetricAnnotationMediator.flushAll();

        boolean anyMatch = false;
        for (String log : sink.getLogEvents()) {
            try {
                ArrayList<String> metricNames = parseMetricNames(log);
                Assertions.assertEquals(
                        "MetricAnnotationMediatorTest.countNamedLogger.Count", metricNames.get(0));
                Assertions.assertEquals(
                        Arrays.asList(1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0),
                        (ArrayList<Double>) parseMetricByName(log, metricNames.get(0)));
                anyMatch = true;
            } catch (Exception e) {
            }
        }
        assertTrue(anyMatch);
    }

    @Test
    void testMultiAnnotationMethod() throws JsonProcessingException {
        multiCountMethod();

        MetricAnnotationMediator.flushAll();

        ArrayList<String> metricNames = parseMetricNames(sink.getLogEvents().get(0));
        Assertions.assertEquals(
                Stream.of("multiCount1", "multiCount2", "multiCount3").collect(Collectors.toSet()),
                metricNames.stream().collect(Collectors.toSet()));
    }

    @Test
    void testCountAndTimeMethod() throws JsonProcessingException {
        countAndTimeMethod();

        MetricAnnotationMediator.flushAll();

        ArrayList<String> metricNames = parseMetricNames(sink.getLogEvents().get(0));
        Assertions.assertEquals(
                Stream.of("Time", "Count").collect(Collectors.toSet()),
                metricNames.stream().collect(Collectors.toSet()));
    }

    @Test
    void testDontLogSuccess() throws NoSuchMethodException {
        MetricAnnotationProcessor.AnnotationTranslator translator =
                getAnnotationTranslatorBuilder().logSuccess(false).build();
        Method method = MetricAnnotationMediatorTest.class.getMethod("dummyMethod");

        MetricAnnotationProcessor.handle(null, method, translator);

        assertTrue(sink.getLogEvents().isEmpty());
    }

    @Test
    void testLogException() throws NoSuchMethodException {
        MetricAnnotationProcessor.AnnotationTranslator translator =
                getAnnotationTranslatorBuilder()
                        .logSuccess(false)
                        .logExceptions(
                                new Class[] {
                                    InvalidMetricException.class, InvalidNamespaceException.class
                                })
                        .build();
        Method method = MetricAnnotationMediatorTest.class.getMethod("dummyMethod");

        MetricAnnotationProcessor.handle(new InvalidDimensionException(""), method, translator);

        assertTrue(sink.getLogEvents().isEmpty());
        sink.getLogEvents().clear();

        MetricAnnotationProcessor.handle(new InvalidMetricException(""), method, translator);

        Assertions.assertFalse(sink.getLogEvents().isEmpty());
        sink.getLogEvents().clear();

        MetricAnnotationProcessor.handle(new InvalidNamespaceException(""), method, translator);

        Assertions.assertFalse(sink.getLogEvents().isEmpty());
    }

    @CountMetric
    void countMethod() {}

    @CountMetric(logger = "example logger")
    void countNamedLogger() {}

    @CountMetric(name = "multiCount1")
    @CountMetric(name = "multiCount2")
    @CountMetric(name = "multiCount3")
    void multiCountMethod() {}

    @CountMetric(name = "Count")
    @ExecutionTimeMetric(name = "Time")
    void countAndTimeMethod() {
        waitRandom(200, 20);
    }

    @ExecutionTimeMetric
    void timeMethod() {
        waitRandom(200, 20);
    }

    @ExecutionTimeMetric(logger = "example logger")
    void multiAnnotationMethod() {
        waitRandom(200, 20);
    }

    public void dummyMethod() {}

    private void waitRandom(int max, int min) {
        int waitTime = random.nextInt(max - min) + min;
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            // Handle interruption if needed
            Thread.currentThread().interrupt();
        }
    }

    protected MetricAnnotationProcessor.AnnotationTranslator.AnnotationTranslatorBuilder
            getAnnotationTranslatorBuilder() {
        return MetricAnnotationProcessor.AnnotationTranslator.builder()
                .name("Translator")
                .logSuccess(true)
                .logExceptions(new Class[] {Throwable.class})
                .flush(true)
                .logger("")
                .value(1)
                .defaultName("defaultName")
                .unit(Unit.NONE);
    }

    @SuppressWarnings("unchecked")
    private ArrayList<String> parseMetricNames(String event) throws JsonProcessingException {
        Map<String, Object> rootNode = parseRootNode(event);
        Map<String, Object> metadata = (Map<String, Object>) rootNode.get("_aws");
        ArrayList<Map<String, Object>> metricDirectives =
                (ArrayList<Map<String, Object>>) metadata.get("CloudWatchMetrics");
        ArrayList<Map<String, String>> metrics =
                (ArrayList<Map<String, String>>) metricDirectives.get(0).get("Metrics");

        ArrayList<String> metricNames = new ArrayList<>();
        for (Map<String, String> metric : metrics) {
            metricNames.add(metric.get("Name"));
        }
        return metricNames;
    }

    @SuppressWarnings("unchecked")
    private Object parseMetricByName(String event, String name) throws JsonProcessingException {
        Map<String, Object> rootNode = parseRootNode(event);
        return rootNode.get(name);
    }

    private Map<String, Object> parseRootNode(String event) throws JsonProcessingException {
        return new JsonMapper().readValue(event, new TypeReference<Map<String, Object>>() {});
    }
}
