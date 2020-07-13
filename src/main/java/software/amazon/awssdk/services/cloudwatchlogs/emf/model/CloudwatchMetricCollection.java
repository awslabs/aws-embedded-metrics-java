package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * A collection of CloudWatch metrics that share the same Dimensions and Dimension Aggregations.
 */
public class CloudwatchMetricCollection {
    private RootNode rootNode;
    private MetricDirective metricDirective;
    private Aggregation defaultAggregation = null;

    CloudwatchMetricCollection(RootNode rootNode, MetricDirective metricDirective) {
        this.rootNode = rootNode;
        this.metricDirective = metricDirective;
    }

    public void setNamespace(String namespace) {
        metricDirective.setNamespace(namespace);
    }

    /**
     * Add a metric with its value and unit.
     * @param name
     * @param value
     * @param unit Valid unit types can be found here: https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html
     */
    public void putMetric(String name, double value, StandardUnit unit) {
        MetricDefinition newMetric = new MetricDefinition();
        newMetric.setName(name);
        newMetric.setUnit(unit);
        metricDirective.getMetrics().add(newMetric);
        rootNode.setMetricOrProperty(name, value);
    }


    /**
     * Add a metric with its value.
     * Unit will default to None.
     * @param name
     * @param value
     */
    public void putMetric(String name, double value) {
        putMetric(name, value, StandardUnit.NONE);
    }

    /**
     * Add a property to this log entry.
     * Properties are additional values that can be associated with metrics.  They will not show up in
     * CloudWatch metrics, but they are searchable in CloudWatch Insights.
     * @param name
     * @param value
     */
    public void putProperty(String name, Object value) {
        rootNode.setMetricOrProperty(name, value);
    }

    /**
     * Add a dimension aggregation.
     * An aggregation is a set of dimensions.
     * @param dimensions
     */
    public Aggregation putDimensionAggregation(String...dimensions) {
        Aggregation aggregation = new Aggregation(rootNode, dimensions);
        metricDirective.putAggregation(aggregation);
        return aggregation;
    }

    /**
     * Add a dimension to the default aggregation.
     * There is only 1 default aggregation for a metric collection.
     * If more aggregations are required, they must be added using addDimensionAggregations, and have
     * their dimension values set using addDimensionValue.
     * @param name
     * @param value
     */
    public void putDimension(String name, Object value) {
        if (defaultAggregation == null) {
            defaultAggregation = new Aggregation(rootNode);
            metricDirective.putAggregation(defaultAggregation);
        }
        defaultAggregation.addDimension(name, value);
    }
}
