/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package software.amazon.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.*;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;

/** Represents the MetricDirective part of the EMF schema. */
@AllArgsConstructor
class MetricDirective {
    @Setter
    @Getter
    @JsonProperty("Namespace")
    private String namespace;

    @JsonIgnore @Setter @Getter @With private Map<String, MetricDefinition> metrics;

    @JsonIgnore
    @Getter(AccessLevel.PROTECTED)
    private List<DimensionSet> dimensions;

    @JsonIgnore
    @Setter
    @Getter(AccessLevel.PROTECTED)
    private DimensionSet defaultDimensions;

    private boolean shouldUseDefaultDimension;

    MetricDirective() {
        namespace = "aws-embedded-metrics";
        metrics = new ConcurrentHashMap<>();
        dimensions = Collections.synchronizedList(new ArrayList<>());
        defaultDimensions = new DimensionSet();
        shouldUseDefaultDimension = true;
    }

    /**
     * Adds a dimension set to the end of the collection.
     *
     * @param dimensionSet
     */
    void putDimensionSet(DimensionSet dimensionSet) {
        // Duplicate dimensions sets are removed before being added to the end of the collection.
        // This ensures only latest dimension value is used as a target member on the root EMF node.
        // This operation is O(n^2), but acceptable given sets are capped at 30 dimensions
        dimensions.removeIf(dim -> dim.getDimensionKeys().equals(dimensionSet.getDimensionKeys()));
        dimensions.add(dimensionSet);
    }

    // Helper method for testing putMetric()
    void putMetric(String key, double value) {
        putMetric(key, value, Unit.NONE, StorageResolution.STANDARD);
    }

    // Helper method for testing putMetric()
    void putMetric(String key, double value, Unit unit) {
        putMetric(key, value, unit, StorageResolution.STANDARD);
    }

    // Helper method for testing serialization
    void putMetric(String key, double value, StorageResolution storageResolution) {
        putMetric(key, value, Unit.NONE, storageResolution);
    }

    void putMetric(String key, double value, Unit unit, StorageResolution storageResolution) {
        metrics.compute(
                key,
                (k, v) -> {
                    if (v == null) return new MetricDefinition(key, unit, storageResolution, value);
                    else {
                        v.addValue(value);
                        return v;
                    }
                });
    }

    @JsonProperty("Metrics")
    Collection<MetricDefinition> getAllMetrics() {
        return metrics.values();
    }

    @JsonProperty("Dimensions")
    List<Set<String>> getAllDimensionKeys() throws DimensionSetExceededException {
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
        dimensions = Collections.synchronizedList(new ArrayList<>(dimensionSets));
    }

    /**
     * Override existing dimensions. Default dimensions are preserved optionally.
     *
     * @param useDefault indicates whether default dimensions should be used
     * @param dimensionSets the dimensionSets to be set
     */
    void setDimensions(boolean useDefault, List<DimensionSet> dimensionSets) {
        shouldUseDefaultDimension = useDefault;
        dimensions = Collections.synchronizedList(new ArrayList<>(dimensionSets));
    }

    /**
     * Clear existing custom dimensions.
     *
     * @param useDefault indicates whether default dimensions should be used
     */
    void resetDimensions(boolean useDefault) {
        shouldUseDefaultDimension = useDefault;
        dimensions = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Return all the dimension sets. If there's a default dimension set, the custom dimensions are
     * prepended with the default dimensions.
     */
    List<DimensionSet> getAllDimensions() throws DimensionSetExceededException {
        if (!shouldUseDefaultDimension) {
            return dimensions;
        }

        if (dimensions.isEmpty()) {
            return Arrays.asList(defaultDimensions);
        }

        List<DimensionSet> allDimensions = new ArrayList<>();
        for (DimensionSet dim : dimensions) {
            allDimensions.add(defaultDimensions.add(dim));
        }

        return allDimensions;
    }

    /**
     * Test if there's any metric added.
     *
     * @return true if no metrics have been added, otherwise, false
     */
    boolean hasNoMetrics() {
        return this.getMetrics().isEmpty();
    }

    /**
     * Create a copy of the metric directive
     *
     * @param preserveDimensions indicates whether the custom dimensions should be preserved
     * @return A metric directive object
     */
    MetricDirective copyWithoutMetrics(boolean preserveDimensions) {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.setDefaultDimensions(this.defaultDimensions);
        metricDirective.setNamespace(this.namespace);
        metricDirective.shouldUseDefaultDimension = this.shouldUseDefaultDimension;

        if (preserveDimensions) {
            this.dimensions.forEach(metricDirective::putDimensionSet);
        }

        return metricDirective;
    }
}
