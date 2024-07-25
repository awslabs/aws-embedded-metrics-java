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
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;

/** Represents the MetricDirective part of the EMF schema. */
@AllArgsConstructor
class MetricDirective {
    @Setter
    @Getter
    @JsonProperty("Namespace")
    private String namespace;

    @JsonIgnore @Setter @Getter @With private Map<String, Metric> metrics;

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
        putMetric(key, value, Unit.NONE, StorageResolution.STANDARD, AggregationType.LIST);
    }

    // Helper method for testing putMetric()
    void putMetric(String key, double value, Unit unit) {
        putMetric(key, value, unit, StorageResolution.STANDARD, AggregationType.LIST);
    }

    // Helper method for testing serialization
    void putMetric(String key, double value, StorageResolution storageResolution) {
        putMetric(key, value, Unit.NONE, storageResolution, AggregationType.LIST);
    }

    void putMetric(
            String key,
            double value,
            Unit unit,
            StorageResolution storageResolution,
            AggregationType aggregationType) {
        metrics.compute(
                key,
                (k, v) -> {
                    if (v == null) {
                        Metric.MetricBuilder builder;
                        switch (aggregationType) {
                            case STATISTIC_SET:
                                builder = StatisticSet.builder();
                                break;
                            case LIST:
                            default:
                                builder = MetricDefinition.builder();
                        }
                        return builder.name(k)
                                .unit(unit)
                                .storageResolution(storageResolution)
                                .addValue(value);
                    } else if (v instanceof Metric.MetricBuilder) {
                        ((Metric.MetricBuilder) v).addValue(value);
                        return v;
                    } else {
                        throw new InvalidMetricException(
                                String.format(
                                        "New metrics cannot be put to the name: \"%s\", because it has been set to an immutable metric type.",
                                        k));
                    }
                });
    }

    /**
     * Sets a metric to the given value. If a metric with the same name already exists, it will be
     * overwritten.
     *
     * @param key the name of the metric
     * @param value the value of the metric
     */
    void setMetric(String key, Metric value) {
        value.setName(key);
        metrics.put(key, value);
    }

    @JsonProperty("Metrics")
    Collection<Metric> getAllMetrics() {
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
