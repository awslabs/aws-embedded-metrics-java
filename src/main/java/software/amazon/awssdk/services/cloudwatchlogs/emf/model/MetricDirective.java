package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/** Represents the MetricDirective part of the EMF schema. */
class MetricDirective {
    @Setter
    @Getter
    @JsonProperty("Namespace")
    private String namespace = "aws-embedded-metrics";

    @Setter
    @Getter
    @JsonProperty("Metrics")
    private List<MetricDefinition> metrics = new ArrayList<>();

    @Getter(AccessLevel.PROTECTED)
    private List<DimensionSet> dimensions = new ArrayList<>();

    @Setter
    @Getter(AccessLevel.PROTECTED)
    private DimensionSet defaultDimensions = new DimensionSet();

    private boolean shouldUseDefaultDimension = true;

    void putDimensionSet(DimensionSet dimensionSet) {
        dimensions.add(dimensionSet);
    }

    void putMetric(MetricDefinition metric) {
        metrics.add(metric);
    }

    @JsonProperty("Dimensions")
    List<Set<String>> getAllDimensionKeys() {
        return getAllDimensions().stream()
                .map(DimensionSet::getDimensionKeys)
                .collect(Collectors.toList());
    }

    /**
     * Override all existing dimensions.
     *
     * @param dimensionSets
     */
    void setDimensions(List<DimensionSet> dimensionSets) {
        shouldUseDefaultDimension = false;
        dimensions = dimensionSets;
    }

    /**
     * Return all the dimension sets. If there's a default dimension set, the custom dimensions are
     * prepended with the default dimensions.
     */
    List<DimensionSet> getAllDimensions() {
        if (!shouldUseDefaultDimension) {
            return dimensions;
        }

        if (dimensions.isEmpty()) {
            return Arrays.asList(defaultDimensions);
        }

        return dimensions.stream()
                .map(dim -> defaultDimensions.add(dim))
                .collect(Collectors.toList());
    }

    /**
     * Test if there's any metric added.
     *
     * @return true if no metrics have been added, otherwise, false
     */
    boolean hasNoMetrics() {
        return this.getMetrics().isEmpty();
    }
}
