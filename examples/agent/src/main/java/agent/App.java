package agent;

import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.environment.DefaultEnvironment;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;

import java.util.concurrent.TimeUnit;

public class App {

    public static void main(String[] args) {
        DefaultEnvironment environment = new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());
        try {
            emitMetric(environment);
            emitMetric(environment);
            emitMetric(environment);
        } catch (InvalidMetricException | InvalidDimensionException | DimensionSetExceededException e) {
            System.out.println(e);
        }

        try {
            environment.getSink().shutdown().get(360_000L, TimeUnit.MILLISECONDS);
        } catch (Exception ignored) {
        }
    }

    private static void emitMetric(Environment environment)
            throws InvalidDimensionException, InvalidMetricException, DimensionSetExceededException {
        MetricsLogger logger = new MetricsLogger(environment);
        logger.setDimensions(DimensionSet.of("Operation", "Agent"));
        logger.putMetric("ExampleMetric", 100, Unit.MILLISECONDS);
        logger.putMetric("ExampleHighResolutionMetric", 10, Unit.MILLISECONDS, StorageResolution.HIGH);
        logger.putProperty("RequestId", "422b1569-16f6-4a03-b8f0-fe3fd9b100f8");
        logger.flush();
    }
}
