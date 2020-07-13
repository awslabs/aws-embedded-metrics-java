package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MetricDirective part of the EMF schema.
 */
class MetricDirective {
    @Setter
    @Getter
    @JsonProperty("Namespace")
    private String namespace;

    @Setter
    @Getter
    @JsonProperty("Metrics")
    private List<MetricDefinition> metrics;

    @Setter
    @Getter
    @JsonProperty("Dimensions")
    private AggregationCollection dimensions =  new AggregationCollection();

    MetricDirective() {
        metrics = new ArrayList<MetricDefinition>();
        namespace = "aws-embedded-metrics";
    }

    /**
     * put a new Dimension Aggregation into the list of aggregations
     * @param aggregation
     */
    void putAggregation(Aggregation aggregation) {
        getDimensions().addAggregation(aggregation);
    }
}
