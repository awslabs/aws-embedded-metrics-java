package emf.canary;

import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class ECSRunnable implements Runnable {

    @Override
    public void run() {
        MetricsLogger logger = new MetricsLogger();

        String version = logger.getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "Unknown";
        }
        Configuration config = EnvironmentConfigurationProvider.getConfig();
        config.setLogGroupName("/Canary/Java/CloudWatchAgent/Metrics");

        try {
            logger.setNamespace("Canary");
            logger.setDimensions(
                    DimensionSet.of(
                            "Runtime", "Java8",
                            "Platform", "ECS",
                            "Agent", "CloudWatchAgent",
                            "Version", version));
        } catch (InvalidNamespaceException | InvalidDimensionException | DimensionSetExceededException e) {
            System.out.println(e);
        }

        MemoryMXBean runtimeMXBean = ManagementFactory.getMemoryMXBean();
        long heapTotal = Runtime.getRuntime().totalMemory();
        long heapUsed = runtimeMXBean.getHeapMemoryUsage().getUsed();
        long nonHeapUsed = runtimeMXBean.getNonHeapMemoryUsage().getUsed();

        try {
            logger.putMetric("Invoke", 1, Unit.COUNT);
            logger.putMetric("Memory.HeapTotal", heapTotal, Unit.COUNT);
            logger.putMetric("Memory.HeapUsed", heapUsed, Unit.COUNT, StorageResolution.HIGH);
            logger.putMetric("Memory.JVMUsedTotal", heapUsed + nonHeapUsed, Unit.COUNT);
        } catch (InvalidMetricException e) {
            System.out.println(e);
        }

        logger.flush();
    }
}
