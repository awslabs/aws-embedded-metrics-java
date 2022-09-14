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

package software.amazon.cloudwatchlogs.emf;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.environment.DefaultEnvironment;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;

public class MetricsLoggerIntegrationTest {

    private Configuration config = EnvironmentConfigurationProvider.getConfig();
    private final String serviceName = "IntegrationTests-" + getLocalHost();
    private final String serviceType = "AutomatedTest";
    private final String logGroupName = "aws-emf-java-integ";
    private final String dimensionName = "Operation";
    private final String dimensionValue = "Integ-Test-Agent";
    private DimensionSet dimensions = DimensionSet.of(dimensionName, dimensionValue);
    private EMFIntegrationTestHelper testHelper = new EMFIntegrationTestHelper();

    public MetricsLoggerIntegrationTest() throws InvalidDimensionException {
    }

    @Before
    public void setUp() {
        config.setServiceName(serviceName);
        config.setServiceType(serviceType);
        config.setLogGroupName(logGroupName);
    }

    @Test(timeout = 120_000)
    public void testSingleFlushOverTCP() throws InterruptedException, InvalidMetricException {
        Environment env = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());
        String metricName = "TCP-SingleFlush";
        int expectedSamples = 1;
        config.setAgentEndpoint("tcp://127.0.0.1:25888");

        logMetric(env, metricName);
        env.getSink().shutdown().join();

        assertTrue(retryUntilSucceed(() -> buildRequest(metricName), expectedSamples));
    }

    @Test(timeout = 300_000)
    public void testMultipleFlushesOverTCP() throws InterruptedException, InvalidMetricException {
        Environment env = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());
        String metricName = "TCP-MultipleFlushes";
        int expectedSamples = 3;
        config.setAgentEndpoint("tcp://127.0.0.1:25888");

        logMetric(env, metricName);
        logMetric(env, metricName);
        Thread.sleep(500);
        logMetric(env, metricName);
        env.getSink().shutdown().join();

        assertTrue(retryUntilSucceed(() -> buildRequest(metricName), expectedSamples));
    }

    @Test(timeout = 120_000)
    public void testSingleFlushOverUDP() throws InterruptedException, InvalidMetricException {
        Environment env = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());
        String metricName = "UDP-SingleFlush";
        int expectedSamples = 1;
        config.setAgentEndpoint("udp://127.0.0.1:25888");

        logMetric(env, metricName);
        env.getSink().shutdown().join();

        assertTrue(retryUntilSucceed(() -> buildRequest(metricName), expectedSamples));
    }

    @Test(timeout = 300_000)
    public void testMultipleFlushOverUDP() throws InterruptedException, InvalidMetricException {
        Environment env = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());
        String metricName = "UDP-MultipleFlush";
        int expectedSamples = 3;
        config.setAgentEndpoint("udp://127.0.0.1:25888");

        logMetric(env, metricName);
        logMetric(env, metricName);
        Thread.sleep(500);
        logMetric(env, metricName);
        env.getSink().shutdown().join();

        assertTrue(retryUntilSucceed(() -> buildRequest(metricName), expectedSamples));
    }

    private void logMetric(Environment env, String metricName) throws InvalidMetricException {
        MetricsLogger logger = new MetricsLogger(env);
        logger.putDimensions(dimensions);
        logger.putMetric(metricName, 100, Unit.MILLISECONDS);
        logger.flush();
    }

    private String getLocalHost() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "UnknownHost";
        }
    }

    private GetMetricStatisticsRequest buildRequest(String metricName) {
        Instant now = Instant.now();
        List<Dimension> dims =
                Arrays.asList(
                        getDimension("ServiceName", serviceName),
                        getDimension("ServiceType", serviceType),
                        getDimension("LogGroup", logGroupName),
                        getDimension(dimensionName, dimensionValue));

        return GetMetricStatisticsRequest.builder()
                .namespace("aws-embedded-metrics")
                .metricName(metricName)
                .dimensions(dims)
                .period(60)
                .startTime(now.minusMillis(5000))
                .endTime(now)
                .statistics(Statistic.SAMPLE_COUNT)
                .build();
    }

    private Dimension getDimension(String name, String value) {
        return Dimension.builder().name(name).value(value).build();
    }

    private boolean retryUntilSucceed(Supplier<GetMetricStatisticsRequest> provider, int expected)
            throws InterruptedException {
        int attempts = 0;
        while (!testHelper.checkMetricExistence(provider.get(), expected)) {
            attempts++;
            System.out.println(
                    "No metrics yet. Sleeping before trying again. Attempt #" + attempts);
            Thread.sleep(2000);
        }
        return true;
    }
}
