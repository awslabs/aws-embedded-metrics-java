package emf.canary;

import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.Unit;

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
        logger.putDimensions(
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
