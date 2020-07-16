package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

/**
 * A runtime environment (e.g. Lambda, EKS, ECS, EC2)
 */
public interface Environment {
    /**
     * Determines whether or not we are executing in this environment
     */
    boolean probe();

    /**
     * Get the environment name. This will be used to set the ServiceName dimension.
     */
    String getName();

    /**
     * Get the environment type. This will be used to set the ServiceType dimension.
     */
    String getType();

    /**
     * Get log group name. This will be used to set the LogGroup dimension.
     */
    String getLogGroupName();

    /**
     * Configure the context with environment properties.
     *
     * @param context
     */
    void configureContext(MetricsContext context);

    /**
     * Create the appropriate sink for this environment.
     */
    ISink getSink();
}
