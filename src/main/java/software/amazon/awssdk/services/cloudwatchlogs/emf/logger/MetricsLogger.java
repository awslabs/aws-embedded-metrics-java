package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.Environment;
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;

public class MetricsLogger {
    private MetricsContext context;
    private Environment environment;

    public MetricsLogger(EnvironmentProvider environmentProvider) {
        this(environmentProvider, new MetricsContext());
    }

    public MetricsLogger(EnvironmentProvider environmentProvider, MetricsContext metricsContext) {
        environment = environmentProvider.resolveEnvironment();
        context = metricsContext;
    }

    /**
     * Flushes the current context state to the configured sink.
     * TODO: Support flush asynchronously
     */
    public void flush() {
        ISink sink = environment.getSink();
        configureContextForEnvironment(context, environment);
        sink.accept(context);
        context = context.createCopyWithContext();
    }

    /**
     * Set a property on the published metrics.
     * This is stored in the emitted log data and you are not
     * charged for this data by CloudWatch Metrics.
     * These values can be values that are useful for searching on,
     * but have too high cardinality to emit as dimensions to
     * CloudWatch Metrics.
     *
     * @param key Property name
     * @param value Property value
     */
    public MetricsLogger putProperty(String key, Object value) {
        this.context.putProperty(key, value);
        return this;
    }

    /**
     * Adds a dimension.
     * This is generally a low cardinality key-value pair that is part of the metric identity.
     * CloudWatch treats each unique combination of dimensions as a separate metric, even if the metrics have the same metric name.
     *
     * @param dimensions
     * @see [CloudWatch Dimensions](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Dimension)
     */
    public MetricsLogger putDimensions(DimensionSet dimensions) {
        context.putDimension(dimensions);
        return this;
    }

    /**
     * Overwrite all dimensions on this MetricsLogger instance.
     *
     * @param dimensionSets
     * @see [CloudWatch Dimensions](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Dimension)
     */
    public MetricsLogger setDimensions(DimensionSet ... dimensionSets) {
        context.setDimensions(dimensionSets);
        return this;
    }

    /**
     * Put a metric value.
     * This value will be emitted to CloudWatch Metrics asyncronously and does not contribute to your
     * account TPS limits. The value will also be available in your CloudWatch Logs
     * @param key
     * @param value
     * @param unit
     */
    public MetricsLogger putMetric(String key, double value, StandardUnit unit) {
        this.context.putMetric(key, value, unit);
        return this;
    }

    /**
     * Put a metric value.
     * This value will be emitted to CloudWatch Metrics asyncronously and does not contribute to your
     * account TPS limits. The value will also be available in your CloudWatch Logs
     * @param key
     * @param value
     */
    public MetricsLogger putMetric(String key, double value) {
        this.context.putMetric(key, value, StandardUnit.NONE);
        return this;
    }

    /**
     * Add a custom key-value pair to the Metadata object.
     * @see [CloudWatch Metadata](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html#CloudWatch_Embedded_Metric_Format_Specification_structure_metadata)
     */
    public MetricsLogger putMetadata(String key, Object value) {
        this.context.putMetadata(key, value);
        return this;
    }

    /**
     * Set the CloudWatch namespace that metrics should be published to.
     * @param namespace
     */
    public MetricsLogger setNamespace(String namespace) {
        this.context.setNamespace(namespace);
        return this;
    }

    private void configureContextForEnvironment(MetricsContext context, Environment environment) {
        if (context.hasDefaultDimensions())
            return;
        DimensionSet defaultDimension = new DimensionSet();
        defaultDimension.addDimension("LogGroup", environment.getLogGroupName());
        defaultDimension.addDimension("ServiceName", environment.getName());
        defaultDimension.addDimension("ServiceType", environment.getType());
        context.setDefaultDimensions(defaultDimension);
        environment.configureContext(context);
    }
}
