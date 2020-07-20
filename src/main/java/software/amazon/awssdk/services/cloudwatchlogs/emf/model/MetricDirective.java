package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents the MetricDirective part of the EMF schema.
 */
class MetricDirective {
    @Setter
    @Getter
    @JsonProperty("Namespace")
    private String namespace = "aws-embedded-metrics";

    @Setter
    @Getter
    @JsonProperty("Metrics")
    private List<MetricDefinition> metrics = new ArrayList<>();

    @Setter
    private List<DimensionSet> dimensions = new ArrayList<>();

    @Setter
    private DimensionSet defaultDimensions = new DimensionSet();
    private boolean should_use_default_dimension = true;


    void putDimensionSet(DimensionSet dimensionSet) {
        dimensions.add(dimensionSet);
    }

    void putMetric(MetricDefinition metric) {
        metrics.add(metric);
    }

    @JsonProperty("Dimensions")
    List<Set<String>> getAllDimensionKeys() {
        return getAllDimensions()
                .stream()
                .map(DimensionSet::getDimensionKeys)
                .collect(Collectors.toList());
    }

    /**
     * Override all existing dimensions.
     * @param dimensionSets
     */
    void setDimensions(List<DimensionSet> dimensionSets) {
        should_use_default_dimension = false;
        dimensions = dimensionSets;
    }

    /**
     * Return all the dimension sets. If there's a default dimension set, the custom dimensions are prepended
     * with the default dimensions.
     */
    List<DimensionSet> getAllDimensions() {
        if (!should_use_default_dimension) {
            return dimensions;
        }

        if (dimensions.isEmpty()) {
            return Arrays.asList(defaultDimensions);
        }

        return dimensions
                .stream()
                .map(dim -> defaultDimensions.add(dim))
                .collect(Collectors.toList());
    }
}
