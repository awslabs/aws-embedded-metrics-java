package software.amazon.awssdk.services.cloudwatchlogs.emf;

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
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.DimensionSet;

public class MetricsLoggerIntegrationTest {

    private Configuration config = EnvironmentConfigurationProvider.getConfig();
    private final String serviceName = "IntegrationTests-" + getLocalHost();
    private final String serviceType = "AutomatedTest";
    private final String logGroupName = "aws-emf-java-integ";
    private final String dimensionName = "Operation";
    private final String dimensionValue = "Integ-Test-Agent";
    private DimensionSet dimensions = DimensionSet.of(dimensionName, dimensionValue);
    private EMFIntegrationTestHelper testHelper = new EMFIntegrationTestHelper();

    @Before
    public void setUp() {
        config.setServiceName(serviceName);
        config.setServiceType(serviceType);
        config.setLogGroupName(logGroupName);
    }

    @Test(timeout = 120_000)
    public void testSingleFlushOverTCP() throws InterruptedException {
        String metricName = "TCP-SingleFlush";
        int expectedSamples = 1;
        config.setAgentEndpoint("tcp://127.0.0.1:25888");

        logMetric(metricName);

        assertTrue(retryUntilSucceed(() -> buildRequest(metricName), expectedSamples));
    }

    @Test(timeout = 300_000)
    public void testMultipleFlushesOverTCP() throws InterruptedException {
        String metricName = "TCP-MultipleFlushes";
        int expectedSamples = 3;
        config.setAgentEndpoint("tcp://127.0.0.1:25888");

        logMetric(metricName);
        logMetric(metricName);
        Thread.sleep(500);
        logMetric(metricName);

        assertTrue(retryUntilSucceed(() -> buildRequest(metricName), expectedSamples));
    }

    @Test(timeout = 120_000)
    public void testSingleFlushOverUDP() throws InterruptedException {
        String metricName = "UDP-SingleFlush";
        int expectedSamples = 1;
        config.setAgentEndpoint("udp://127.0.0.1:25888");

        logMetric(metricName);

        assertTrue(retryUntilSucceed(() -> buildRequest(metricName), expectedSamples));
    }

    @Test(timeout = 300_000)
    public void testMultipleFlushOverUDP() throws InterruptedException {
        String metricName = "UDP-MultipleFlush";
        int expectedSamples = 3;
        config.setAgentEndpoint("udp://127.0.0.1:25888");

        logMetric(metricName);
        logMetric(metricName);
        Thread.sleep(500);
        logMetric(metricName);

        assertTrue(retryUntilSucceed(() -> buildRequest(metricName), expectedSamples));
    }

    private void logMetric(String metricName) {
        MetricsLogger logger = new MetricsLogger(new EnvironmentProvider());
        logger.putDimensions(dimensions);
        logger.putMetric(metricName, 100, StandardUnit.MILLISECONDS);
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
        List<Dimension> dimensions =
                Arrays.asList(
                        getDimension("ServiceName", serviceName),
                        getDimension("ServiceType", serviceType),
                        getDimension("LogGroup", logGroupName),
                        getDimension(dimensionName, dimensionValue));

        return GetMetricStatisticsRequest.builder()
                .namespace("aws-embedded-metrics")
                .metricName(metricName)
                .dimensions(dimensions)
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
