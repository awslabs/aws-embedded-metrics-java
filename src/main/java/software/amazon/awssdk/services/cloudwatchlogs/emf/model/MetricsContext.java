package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/** Stores metrics and their associated properties and dimensions. */
public class MetricsContext {

    @Getter private RootNode rootNode;

    private MetricDirective metricDirective;

    public MetricsContext() {
        this(new RootNode());
    }

    public MetricsContext(RootNode rootNode) {
        this.rootNode = rootNode;
        metricDirective = rootNode.getAws().createMetricDirective();
    }

    public MetricsContext(
            String namespace,
            Map<String, Object> properties,
            List<DimensionSet> dimensionSets,
            DimensionSet defaultDimensionSet) {
        this();
        setNamespace(namespace);
        setDefaultDimensions(defaultDimensionSet);
        for (DimensionSet dimension : dimensionSets) {
            putDimension(dimension);
        }
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            putProperty(entry.getKey(), entry.getValue());
        }
    }

    /** Return the namespace. If the namespace is not set, it would return a default value. */
    public String getNamespace() {
        return metricDirective.getNamespace();
    }

    /**
     * Update the namespace with the parameter.
     *
     * @param namespace The new namespace
     */
    public void setNamespace(String namespace) {
        metricDirective.setNamespace(namespace);
    }

    /** Return the default dimension set. */
    public DimensionSet getDefaultDimensions() {
        return metricDirective.getDefaultDimensions();
    }

    /**
     * Sets default dimensions for all other dimensions that get added to the context. If no custom
     * dimensions are specified, the metrics will be emitted with the defaults. If custom dimensions
     * are specified, they will be prepended with the default dimensions
     *
     * @param dimensionSet
     */
    public void setDefaultDimensions(DimensionSet dimensionSet) {
        metricDirective.setDefaultDimensions(dimensionSet);
    }

    /** Return true if there're default dimensions defined, otherwise, false. */
    public boolean hasDefaultDimensions() {
        return getDefaultDimensions().getDimensionKeys().size() > 0;
    }

    /**
     * Add a metric measurement to the context. Multiple calls using the same key will be stored as
     * an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Latency", 100, StandardUnit.MILLISECONDS)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param unit The unit of the metric
     */
    public void putMetric(String key, double value, StandardUnit unit) {
        metricDirective.putMetric(new MetricDefinition(key, unit));
        rootNode.putMetric(key, value);
    }

    /**
     * Add a metric measurement to the context without a unit Multiple calls using the same key will
     * be stored as an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Count", 10)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     */
    public void putMetric(String key, double value) {
        putMetric(key, value, StandardUnit.NONE);
    }

    /**
     * Add a property to this log entry. Properties are additional values that can be associated
     * with metrics. They will not show up in CloudWatch metrics, but they are searchable in
     * CloudWatch Insights.
     *
     * <pre>{@code
     * metricContext.putProperty("Location", 'US')
     * }</pre>
     *
     * @param name Name of the property
     * @param value Value of the property
     */
    public void putProperty(String name, Object value) {
        rootNode.putProperty(name, value);
    }

    /** Return the value of a property. */
    public Object getProperty(String name) {
        return rootNode.getProperties().get(name);
    }

    /**
     * Add dimensions to the metric context.
     *
     * <pre>{@code
     * metricContext.putDimension(DimensionSet.of("Dim", "Value" ))
     * }</pre>
     *
     * @param dimensionSet
     */
    public void putDimension(DimensionSet dimensionSet) {
        metricDirective.putDimensionSet(dimensionSet);
    }

    /**
     * Add a dimension set with single dimension-value entry to the metric context.
     *
     * <pre>{@code
     * metricContext.putDimension("Dim", "Value" )
     * }</pre>
     */
    public void putDimension(String dimension, String value) {
        metricDirective.putDimensionSet(DimensionSet.of(dimension, value));
    }

    /** Return the list of dimensions that has been added, including default dimensions. */
    public List<DimensionSet> getDimensions() {
        return metricDirective.getAllDimensions();
    }

    /**
     * Update the dimensions.
     *
     * @param dimensionSets
     */
    public void setDimensions(DimensionSet... dimensionSets) {
        metricDirective.setDimensions(Arrays.asList(dimensionSets));
    }

    /** Add a key-value pair to the metadata. */
    public void putMetadata(String key, Object value) {
        rootNode.getAws().putCustomMetadata(key, value);
    }

    /** Creates an independently flushable context. */
    public MetricsContext createCopyWithContext() {
        return new MetricsContext(
                this.metricDirective.getNamespace(),
                this.rootNode.getProperties(),
                this.metricDirective.getDimensions(),
                this.metricDirective.getDefaultDimensions());
    }

    /**
     * Serialize the metrics in this context to a string.
     *
     * @throws JsonProcessingException
     */
    public String serialize() throws JsonProcessingException {
        return this.rootNode.serialize();
    }
}
