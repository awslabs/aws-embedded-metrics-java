package emf.canary;

import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.DimensionSet;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.Optional;

public class ECSRunnable implements Runnable {

    @Override
    public void run() {
        MetricsLogger logger = new MetricsLogger(new EnvironmentProvider());

        String version = logger.getClass().getPackage().getImplementationVersion();
        if (version == null) {
            version = "Unknown";
        }
        Configuration config = EnvironmentConfigurationProvider.getConfig();
        config.setLogGroupName(Optional.of("/Canary/Java/CloudWatchAgent/Metrics"));
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

        logger.putMetric("Invoke", 1, StandardUnit.COUNT);
        logger.putMetric("Memory.HeapTotal", heapTotal, StandardUnit.COUNT);
        logger.putMetric("Memory.HeapUsed", heapUsed, StandardUnit.COUNT);

        // Java does not have a way to get RSS directly. Use the sume of heap and non-heap memory size.
        // This is not actual RSS as the used memory may be swapped.
        logger.putMetric("Memory.RSS", heapUsed + nonHeapUsed, StandardUnit.COUNT);

        logger.flush();
    }
}
