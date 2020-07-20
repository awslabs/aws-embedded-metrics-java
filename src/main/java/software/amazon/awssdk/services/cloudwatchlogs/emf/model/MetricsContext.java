package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import lombok.Getter;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.util.Arrays;

/**
 * Stores metrics and their associated properties and dimensions
 */
public class MetricsContext {

    @Getter
    private RootNode rootNode;

    private MetricDirective metricDirective;


    public MetricsContext() {
        this(new RootNode());
    }

    public MetricsContext(RootNode rootNode) {
        this.rootNode = rootNode;
        metricDirective = rootNode.getAws().createMetricDirective();
    }

    /**
     * Update the namespace with the parameter
     * @param namespace The new namespace
     */
    public void setNamespace(String namespace) {
        metricDirective.setNamespace(namespace);
    }

    /**
     * Update the dimensions
     * @param dimensionSets
     */
    public void setDimensions(DimensionSet ... dimensionSets) {
        metricDirective.setDimensions(Arrays.asList(dimensionSets));
    }

    /**
     * Sets default dimensions for all other dimensions that get added
     * to the context.
     * If no custom dimensions are specified, the metrics will be emitted
     * with the defaults.
     * If custom dimensions are specified, they will be prepended with
     * the default dimensions
     * @param dimensionSet
     */
    public void setDefaultDimensions(DimensionSet dimensionSet) {
        metricDirective.setDefaultDimensions(dimensionSet);
    }

    /**
     * Add a metric measurement to the context.
     * Multiple calls using the same key will be stored as an
     * array of scalar values
     *
     *  <pre>
     *  {@code
     *    metricContext.putMetric("Latency", 100, StandardUnit.MILLISECONDS)
     *  }
     *  </pre>
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
     * Add a metric measurement to the context without a unit
     * Multiple calls using the same key will be stored as an
     * array of scalar values
     *
     *  <pre>
     *  {@code
     *    metricContext.putMetric("Count", 10)
     *  }
     *  </pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     */
    public void putMetric(String key, double value) {
        putMetric(key, value, StandardUnit.NONE);
    }

    /**
     * Add a property to this log entry.
     * Properties are additional values that can be associated with metrics.  They will not show up in
     * CloudWatch metrics, but they are searchable in CloudWatch Insights.
     *
     *  <pre>
     *  {@code
     *    metricContext.putProperty("Location", 'US')
     *  }
     *  </pre>
     *
     * @param name Name of the property
     * @param value Value of the property
     */
    public void putProperty(String name, Object value) {
        rootNode.putProperty(name, value);

    }


    /**
     * Add dimensions to the metric context
     *
     * <pre>
     * {@code
     *     metricContext.putDimension(DimensionSet.of("Dim", "Value" ))
     * }
     * </pre>
     * @param dimensionSet
     */

    public void putDimension(DimensionSet dimensionSet) {
        metricDirective.putDimensionSet(dimensionSet);
    }

    /**
     * Add a dimension set with single dimension-value entry to the metric context.
     *
     * <pre>
     * {@code
     *     metricContext.putDimension("Dim", "Value" )
     * }
     * </pre>
     */
    public void putDimension(String dimension, String value) {
        metricDirective.putDimensionSet(DimensionSet.of(dimension, value));
    }
}
