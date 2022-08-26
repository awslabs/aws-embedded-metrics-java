package emf.canary;

import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.environment.Environments;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;

public class ECSRunnable implements Runnable {

    @Override
    public void run() {
        final Configuration config = EnvironmentConfigurationProvider.getConfig();
        config.setEnvironmentOverride(Environments.ECS);
        config.setLogGroupName("/Canary/Java/CloudWatchAgent/Metrics");

        MetricsLogger logger = new MetricsLogger();

        String version = logger.getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "Unknown";
        }

        logger.setNamespace("Canary");
        logger.setDimensions(
                DimensionSet.of(
                        "Runtime", "Java8",
                        "Platform", "ECS",
                        "Agent", "CloudWatchAgent",
                        "Version", version));

        MemoryMXBean runtimeMXBean = ManagementFactory.getMemoryMXBean();
        long heapTotal = Runtime.getRuntime().totalMemory();
        long heapUsed = runtimeMXBean.getHeapMemoryUsage().getUsed();
        long nonHeapUsed = runtimeMXBean.getNonHeapMemoryUsage().getUsed();

        logger.putMetric("Invoke", 1, Unit.COUNT);
        logger.putMetric("Memory.HeapTotal", heapTotal, Unit.COUNT);
        logger.putMetric("Memory.HeapUsed", heapUsed, Unit.COUNT);
        logger.putMetric("Memory.JVMUsedTotal", heapUsed + nonHeapUsed, Unit.COUNT);

        logger.flush();
    }
}
