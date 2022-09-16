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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidTimestampException;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.SinkShunt;

class MetricsLoggerTest {
    private MetricsLogger logger;
    private EnvironmentProvider envProvider;
    private SinkShunt sink;
    private Environment environment;

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
    }

    @Test
    void putProperty_setsProperty() {
        String propertyName = "Property";
        String propertyValue = "PropValue";
        logger.putProperty(propertyName, propertyValue);
        logger.flush();

        assertEquals(propertyValue, sink.getContext().getProperty(propertyName));
    }

    @Test
    void putDimensions_setsDimension() throws InvalidDimensionException {
        String dimensionName = "dim";
        String dimensionValue = "dimValue";
        logger.putDimensions(DimensionSet.of(dimensionName, dimensionValue));
        logger.flush();

        assertEquals(1, sink.getContext().getDimensions().size());
        assertEquals(
                dimensionValue,
                sink.getContext().getDimensions().get(0).getDimensionValue(dimensionName));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "ƊĪⱮḔǸŠƗȌŅ", ":dim"})
    void whenSetDimension_withInvalidName_thenThrowInvalidDimensionException(String dimensionName) {
        assertThrows(
                InvalidDimensionException.class, () -> DimensionSet.of(dimensionName, "dimValue"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "ṼẬḺƯỂ"})
    void whenSetDimension_withInvalidValue_thenThrowInvalidDimensionException(
            String dimensionValue) {
        assertThrows(
                InvalidDimensionException.class, () -> DimensionSet.of("dimName", dimensionValue));
    }

    @Test
    void whenSetDimension_withNameTooLong_thenThrowDimensionException() {
        String dimensionName = "a".repeat(Constants.MAX_DIMENSION_NAME_LENGTH + 1);
        String dimensionValue = "dimValue";
        assertThrows(
                InvalidDimensionException.class,
                () -> DimensionSet.of(dimensionName, dimensionValue));
    }

    @Test
    void whenSetDimension_withValueTooLong_thenThrowDimensionException() {
        String dimensionName = "dim";
        String dimensionValue = "a".repeat(Constants.MAX_DIMENSION_VALUE_LENGTH + 1);
        assertThrows(
                InvalidDimensionException.class,
                () -> DimensionSet.of(dimensionName, dimensionValue));
    }

    @Test
    void whenSetDimension_withNullName_thenThrowDimensionException() {
        assertThrows(InvalidDimensionException.class, () -> DimensionSet.of(null, "dimValue"));
    }

    @Test
    void setDefaultDimensions_overridesDefaultDimensions() throws InvalidDimensionException {
        String dimensionName = "dim";
        String dimensionValue = "dimValue";
        String defaultDimName = "defaultDim";
        String defaultDimValue = "defaultDimValue";

        MetricsContext metricsContext = new MetricsContext();
        metricsContext.setDefaultDimensions(DimensionSet.of(defaultDimName, defaultDimValue));
        metricsContext.setDimensions(DimensionSet.of(dimensionName, dimensionValue));
        logger = new MetricsLogger(envProvider, metricsContext);
        logger.setDimensions(DimensionSet.of(dimensionName, dimensionValue));
        logger.flush();

        assertEquals(1, sink.getContext().getDimensions().size());
        assertNull(sink.getContext().getDimensions().get(0).getDimensionValue(defaultDimName));
    }

    @Test
    void resetDimensions_resetsDimensionsWithDefaultDimensions() throws InvalidDimensionException {
        String dimensionName = "dim";
        String dimensionValue = "dimValue";
        logger.putDimensions(DimensionSet.of("foo", "bar"));
        logger.resetDimensions(true);
        logger.putDimensions(DimensionSet.of(dimensionName, dimensionValue));
        logger.flush();

        assertEquals(1, sink.getContext().getDimensions().size());
        assertEquals(4, sink.getContext().getDimensions().get(0).getDimensionKeys().size());
        assertEquals(
                sink.getContext().getDimensions().get(0).getDimensionValue(dimensionName),
                dimensionValue);
    }

    @Test
    void resetDimensions_resetsDimensionsWithoutDefaultDimensions()
            throws InvalidDimensionException {
        String dimensionName = "dim";
        String dimensionValue = "dimValue";
        logger.putDimensions(DimensionSet.of("foo", "bar"));
        logger.resetDimensions(false);
        logger.putDimensions(DimensionSet.of(dimensionName, dimensionValue));
        logger.flush();

        assertEquals(1, sink.getContext().getDimensions().size());
        assertEquals(1, sink.getContext().getDimensions().get(0).getDimensionKeys().size());
        assertEquals(
                sink.getContext().getDimensions().get(0).getDimensionValue(dimensionName),
                dimensionValue);
    }

    @Test
    void setDimensions_overridesPreviousDimensions() throws InvalidDimensionException {

        String dimensionName = "dim";
        String dimensionValue = "dimValue";
        logger.putDimensions(DimensionSet.of("foo", "bar"));
        logger.setDimensions(DimensionSet.of(dimensionName, dimensionValue));
        logger.flush();

        assertEquals(1, sink.getContext().getDimensions().size());
        assertEquals(1, sink.getContext().getDimensions().get(0).getDimensionKeys().size());
        assertEquals(
                dimensionValue,
                sink.getContext().getDimensions().get(0).getDimensionValue(dimensionName));
    }

    @Test
    void setDimensions_overridesPreviousDimensionsAndPreservesDefault()
            throws InvalidDimensionException {
        String dimensionName = "dim";
        String dimensionValue = "dimValue";
        logger.putDimensions(DimensionSet.of("foo", "bar"));
        logger.setDimensions(true, DimensionSet.of(dimensionName, dimensionValue));
        logger.flush();

        assertEquals(1, sink.getContext().getDimensions().size());
        assertEquals(4, sink.getContext().getDimensions().get(0).getDimensionKeys().size());
        assertEquals(
                sink.getContext().getDimensions().get(0).getDimensionValue(dimensionName),
                dimensionValue);
    }

    @Test
    void setDimensions_clearsDefaultDimensions() throws InvalidMetricException {
        MetricsLogger logger = new MetricsLogger(envProvider);
        logger.setDimensions();
        logger.putMetric("Count", 1);
        logger.flush();
        List<DimensionSet> dimensions = sink.getContext().getDimensions();

        assertEquals(0, dimensions.size());
        assertEquals(1, sink.getLogEvents().size());

        String logEvent = sink.getLogEvents().get(0);
        assertTrue(logEvent.contains("\"Dimensions\":[]"));
    }

    @Test
    void flush_PreservesDimensions() throws InvalidDimensionException {
        MetricsLogger logger = new MetricsLogger(envProvider);
        logger.setDimensions(DimensionSet.of("Name", "Test"));
        logger.flush();
        expectDimension("Name", "Test");

        logger.flush();
        expectDimension("Name", "Test");
    }

    @Test
    void flush_doesNotPreserveDimensions() throws InvalidDimensionException {
        logger.putDimensions(DimensionSet.of("Name", "Test"));
        logger.setFlushPreserveDimensions(false);

        logger.flush();
        assertEquals(4, sink.getContext().getDimensions().get(0).getDimensionKeys().size());
        expectDimension("Name", "Test");

        logger.flush();
        assertEquals(3, sink.getContext().getDimensions().get(0).getDimensionKeys().size());
        expectDimension("Name", null);
    }

    @Test
    void setDimensions_clearsAllDimensions() {
        MetricsLogger logger = new MetricsLogger(envProvider);

        logger.setDimensions();
        logger.flush();

        List<DimensionSet> dimensions = sink.getContext().getDimensions();
        assertEquals(0, dimensions.size());
    }

    @Test
    void whenSetDimensions_withMultipleFlush_thenClearsDimensions() {
        MetricsLogger logger = new MetricsLogger(envProvider);

        logger.setDimensions();
        logger.flush();

        assertEquals(0, sink.getContext().getDimensions().size());

        logger.flush();
        assertEquals(0, sink.getContext().getDimensions().size());
    }

    @Test
    void whenPutMetric_withTooLongName_thenThrowInvalidMetricException() {
        String name1 = "a".repeat(Constants.MAX_METRIC_NAME_LENGTH + 1);
        assertThrows(InvalidMetricException.class, () -> logger.putMetric(name1, 1));
    }

    @Test
    void whenPutMetric_withNullName_thenThrowInvalidMetricException() {
        assertThrows(InvalidMetricException.class, () -> logger.putMetric(null, 1));
    }

    @Test
    void whenPutMetric_withEmptyName_thenThrowInvalidMetricException() {
        assertThrows(InvalidMetricException.class, () -> logger.putMetric("", 1));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void whenPutMetric_withInvalidValue_thenThrowInvalidMetricException(double value) {
        EnvironmentProvider envProvider = mock(EnvironmentProvider.class);
        MetricsLogger logger = new MetricsLogger(envProvider);
        assertThrows(InvalidMetricException.class, () -> logger.putMetric("name", value));
    }

    @Test
    void whenPutMetric_withNullUnit_thenThrowInvalidMetricException() {
        assertThrows(InvalidMetricException.class, () -> logger.putMetric("test", 1, null));
    }

    @Test
    void setNamespace_setsNamespace() throws InvalidNamespaceException {

        String namespace = "testNamespace";
        logger.setNamespace(namespace);
        logger.flush();

        assertEquals(namespace, sink.getContext().getNamespace());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", "ṆẪⱮḖⱾⱣǞḈȄ"})
    void whenSetNamespace_withInvalidValue_thenThrowInvalidNamespaceException(String namespace) {
        EnvironmentProvider envProvider = mock(EnvironmentProvider.class);
        MetricsLogger logger = new MetricsLogger(envProvider);
        assertThrows(InvalidNamespaceException.class, () -> logger.setNamespace(namespace));
    }

    @Test
    void whenSetNamespace_withNameTooLong_thenThrowInvalidNamespaceException() {
        String namespace = "a".repeat(Constants.MAX_NAMESPACE_LENGTH + 1);
        assertThrows(InvalidNamespaceException.class, () -> logger.setNamespace(namespace));
    }

    @Test
    void flush_usesDefaultTimestamp() {
        logger.flush();
        assertNotNull(sink.getContext().getTimestamp());
    }

    @Test
    void setTimestamp_setsTimestamp() throws InvalidTimestampException {
        Instant now = Instant.now();
        logger.setTimestamp(now);
        logger.flush();

        assertEquals(now, sink.getContext().getTimestamp());
    }

    @Test
    void whenSetTimestamp_withInvalidValueInFuture_thenThrowException() {
        Instant now = Instant.now();
        Instant invalidTimestamp = now.plusSeconds(Constants.MAX_TIMESTAMP_FUTURE_AGE_SECONDS + 1);
        assertThrows(InvalidTimestampException.class, () -> logger.setTimestamp(invalidTimestamp));
    }

    @Test
    void whenSetTimestamp_withInvalidValueInPast_thenThrowException() {
        Instant now = Instant.now();
        Instant invalidTimestamp = now.minusSeconds(Constants.MAX_TIMESTAMP_PAST_AGE_SECONDS + 1);
        assertThrows(InvalidTimestampException.class, () -> logger.setTimestamp(invalidTimestamp));
    }

    @Test
    void setTimestamp_withValidValueInFuture() throws InvalidTimestampException {
        Instant now = Instant.now();
        Instant validTimestamp = now.plusSeconds(Constants.MAX_TIMESTAMP_FUTURE_AGE_SECONDS - 1);
        logger.setTimestamp(validTimestamp);
        logger.flush();

        assertEquals(validTimestamp, sink.getContext().getTimestamp());
    }

    @Test
    void setTimestamp_withValidValueInPast() throws InvalidTimestampException {
        Instant now = Instant.now();
        Instant validTimestamp = now.minusSeconds(Constants.MAX_TIMESTAMP_PAST_AGE_SECONDS - 1);
        logger.setTimestamp(validTimestamp);
        logger.flush();

        assertEquals(validTimestamp, sink.getContext().getTimestamp());
    }

    @Test
    void testFlushWithConfiguredServiceName() {
        String serviceName = "TestServiceName";
        when(environment.getName()).thenReturn(serviceName);
        logger.flush();

        expectDimension("ServiceName", serviceName);
    }

    @Test
    void testFlushWithConfiguredServiceType() {
        String serviceType = "TestServiceType";
        when(environment.getType()).thenReturn(serviceType);
        logger.flush();

        expectDimension("ServiceType", serviceType);
    }

    @Test
    void testFlushWithConfiguredLogGroup() {
        String logGroup = "MyLogGroup";
        when(environment.getLogGroupName()).thenReturn(logGroup);
        logger.flush();

        expectDimension("LogGroup", logGroup);
    }

    @Test
    void testFlushWithDefaultDimensionDefined() throws InvalidDimensionException {
        MetricsContext metricsContext = new MetricsContext();
        metricsContext.setDefaultDimensions(DimensionSet.of("foo", "bar"));
        logger = new MetricsLogger(envProvider, metricsContext);
        String logGroup = "MyLogGroup";
        when(environment.getLogGroupName()).thenReturn(logGroup);
        logger.flush();

        expectDimension("foo", "bar");
        expectDimension("LogGroup", null);
    }

    @SuppressWarnings("")
    @Test
    void testUseDefaultEnvironmentOnResolverException() {
        String serviceType = "TestServiceType";
        CompletableFuture<Environment> future =
                CompletableFuture.supplyAsync(
                        () -> {
                            throw new RuntimeException("UnExpected");
                        });
        EnvironmentProvider envProvider = mock(EnvironmentProvider.class);
        when(envProvider.resolveEnvironment()).thenReturn(future);
        when(envProvider.getDefaultEnvironment()).thenReturn(environment);
        when(environment.getType()).thenReturn(serviceType);
        MetricsLogger logger = new MetricsLogger(envProvider);
        logger.flush();

        verify(envProvider).getDefaultEnvironment();
        expectDimension("ServiceType", serviceType);
    }

    @Test
    void flush_doesNotPreserveMetrics() throws InvalidMetricException, InvalidDimensionException {
        MetricsLogger logger = new MetricsLogger(envProvider);
        logger.setDimensions(DimensionSet.of("Name", "Test"));
        logger.putMetric("Count", 1.0);
        logger.flush();
        assertTrue(sink.getLogEvents().get(0).contains("Count"));

        logger.flush();
        assertFalse(sink.getLogEvents().get(0).contains("Count"));
    }

    private void expectDimension(String dimension, String value) {
        List<DimensionSet> dimensions = sink.getContext().getDimensions();
        assertEquals(1, dimensions.size());
        assertEquals(value, dimensions.get(0).getDimensionValue(dimension));
    }
}
