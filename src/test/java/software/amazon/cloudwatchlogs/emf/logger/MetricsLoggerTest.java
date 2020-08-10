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
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.SinkShunt;

public class MetricsLoggerTest {
    private MetricsLogger logger;
    private EnvironmentProvider envProvider;
    private SinkShunt sink;
    private Environment environment;

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
    public void testPutProperty() {
        String propertyName = "Property";
        String propertyValue = "PropValue";
        logger.putProperty(propertyName, propertyValue);
        logger.flush();

        Assert.assertEquals(sink.getContext().getProperty(propertyName), propertyValue);
    }

    @Test
    public void testPutDimension() {
        String dimensionName = "dim";
        String dimensionValue = "dimValue";
        logger.putDimensions(DimensionSet.of(dimensionName, dimensionValue));
        logger.flush();

        Assert.assertEquals(sink.getContext().getDimensions().size(), 1);
        Assert.assertEquals(
                sink.getContext().getDimensions().get(0).getDimensionValue(dimensionName),
                dimensionValue);
    }

    @Test
    public void testOverrideDefaultDimensions() {
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

        Assert.assertEquals(sink.getContext().getDimensions().size(), 1);
        Assert.assertEquals(
                sink.getContext().getDimensions().get(0).getDimensionValue(defaultDimName), null);
    }

    @Test
    public void testOverridePreviousDimensions() {

        String dimensionName = "dim";
        String dimensionValue = "dimValue";
        logger.putDimensions(DimensionSet.of("foo", "bar"));
        logger.setDimensions(DimensionSet.of(dimensionName, dimensionValue));
        logger.flush();

        Assert.assertEquals(sink.getContext().getDimensions().size(), 1);
        Assert.assertEquals(sink.getContext().getDimensions().get(0).getDimensionKeys().size(), 1);
        Assert.assertEquals(
                sink.getContext().getDimensions().get(0).getDimensionValue(dimensionName),
                dimensionValue);
    }

    @Test
    public void testSetNamespace() {

        String namespace = "testNamespace";
        logger.setNamespace(namespace);
        logger.flush();

        Assert.assertEquals(sink.getContext().getNamespace(), namespace);
    }

    @Test
    public void testFlushWithConfiguredServiceName() {
        String serviceName = "TestServiceName";
        when(environment.getName()).thenReturn(serviceName);
        logger.flush();

        expectDimension("ServiceName", serviceName);
    }

    @Test
    public void testFlushWithConfiguredServiceType() {
        String serviceType = "TestServiceType";
        when(environment.getType()).thenReturn(serviceType);
        logger.flush();

        expectDimension("ServiceType", serviceType);
    }

    @Test
    public void testFlushWithConfiguredLogGroup() {
        String logGroup = "MyLogGroup";
        when(environment.getLogGroupName()).thenReturn(logGroup);
        logger.flush();

        expectDimension("LogGroup", logGroup);
    }

    @Test
    public void testFlushWithDefaultDimensionDefined() {
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
    public void testUseDefaultEnvironmentOnResolverException() {
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

    private void expectDimension(String dimension, String value) {
        List<DimensionSet> dimensions = sink.getContext().getDimensions();
        assertEquals(dimensions.size(), 1);
        assertEquals(dimensions.get(0).getDimensionValue(dimension), value);
    }
}
