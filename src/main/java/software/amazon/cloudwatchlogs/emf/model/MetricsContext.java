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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.Instant;
import java.util.*;
import lombok.Getter;
import software.amazon.cloudwatchlogs.emf.Constants;

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

    public MetricsContext(MetricDirective metricDirective) {
        this();
        this.rootNode.getAws().setMetricDirective(metricDirective);
        this.metricDirective = metricDirective;
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

    /** @return the namespace. If the namespace is not set, it would return a default value. */
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

    /** @return the default dimension set. */
    public DimensionSet getDefaultDimensions() {
        return metricDirective.getDefaultDimensions();
    }

    /**
     * Sets default dimensions for all other dimensions that get added to the context. If no custom
     * dimensions are specified, the metrics will be emitted with the defaults. If custom dimensions
     * are specified, they will be prepended with the default dimensions
     *
     * @param dimensionSet the DimensionSet to be the default
     */
    public void setDefaultDimensions(DimensionSet dimensionSet) {
        metricDirective.setDefaultDimensions(dimensionSet);
    }

    public boolean hasDefaultDimensions() {
        return getDefaultDimensions().getDimensionKeys().size() > 0;
    }

    /**
     * Add a metric measurement to the context. Multiple calls using the same key will be stored as
     * an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Latency", 100, Unit.MILLISECONDS)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param unit The unit of the metric
     */
    public void putMetric(String key, double value, Unit unit) {
        metricDirective.putMetric(key, value, unit);
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
        putMetric(key, value, Unit.NONE);
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
     * @param dimensionSet the dimensions set to add
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
     *
     * @param dimension the name of the dimension
     * @param value the value associated with the dimension
     */
    public void putDimension(String dimension, String value) {
        metricDirective.putDimensionSet(DimensionSet.of(dimension, value));
    }

    /** @return the list of dimensions that has been added, including default dimensions. */
    public List<DimensionSet> getDimensions() {
        return metricDirective.getAllDimensions();
    }

    /**
     * Update the dimensions. This would override default dimensions
     *
     * @param dimensionSets the dimensionSets to be set
     */
    public void setDimensions(DimensionSet... dimensionSets) {
        metricDirective.setDimensions(Arrays.asList(dimensionSets));
    }

    /**
     * Update the dimensions. Default dimensions are preserved optionally.
     *
     * @param useDefault indicates whether default dimensions should be used
     * @param dimensionSets the dimensionSets to set
     */
    public void setDimensions(boolean useDefault, DimensionSet... dimensionSets) {
        metricDirective.setDimensions(useDefault, Arrays.asList(dimensionSets));
    }

    /**
     * Reset the dimensions. This would clear all custom dimensions.
     *
     * @param useDefault indicates whether default dimensions should be used
     */
    public void resetDimensions(boolean useDefault) {
        metricDirective.resetDimensions(useDefault);
    }

    /**
     * Add a key-value pair to the metadata
     *
     * @param key the name of the key
     * @param value the value associated with the key
     */
    public void putMetadata(String key, Object value) {
        rootNode.getAws().putCustomMetadata(key, value);
    }

    /** @return timestamp field from the metadata. */
    public Instant getTimestamp() {
        return rootNode.getAws().getTimestamp();
    }

    /**
     * Update timestamp field in the metadata
     *
     * @param timestamp value of timestamp to be set
     */
    public void setTimestamp(Instant timestamp) {
        rootNode.getAws().setTimestamp(timestamp);
    }

    /** @return Creates an independently flushable context. */
    public MetricsContext createCopyWithContext() {
        return new MetricsContext(metricDirective.copyWithoutMetrics());
    }

    /** @return Creates an independently flushable context without metrics and custom dimensions */
    public MetricsContext createCopyWithContextWithoutDimensions() {
        return new MetricsContext(metricDirective.copyWithoutMetricsAndDimensions());
    }

    /**
     * Serialize the metrics in this context to strings. The EMF backend requires no more than 100
     * metrics in one log event. If there're more than 100 metrics, we split the metrics into
     * multiple log events.
     *
     * <p>If a metric has more than 100 data points, we also split the metric.
     *
     * @return the serialized strings.
     * @throws JsonProcessingException if there's any object that cannot be serialized
     */
    public List<String> serialize() throws JsonProcessingException {
        if (rootNode.metrics().size() <= Constants.MAX_METRICS_PER_EVENT
                && !anyMetricWithTooManyDataPoints(rootNode)) {
            return Arrays.asList(this.rootNode.serialize());
        } else {
            List<RootNode> nodes = new ArrayList<>();
            Map<String, MetricDefinition> metrics = new HashMap<>();
            Queue<MetricDefinition> metricDefinitions =
                    new LinkedList<>(rootNode.metrics().values());
            while (metricDefinitions.size() > 0) {
                MetricDefinition metric = metricDefinitions.poll();

                if (metrics.size() == Constants.MAX_METRICS_PER_EVENT
                        || metrics.containsKey(metric.getName())) {
                    nodes.add(buildRootNode(metrics));
                    metrics = new HashMap<>();
                }

                if (metric.getValues().size() <= Constants.MAX_DATAPOINTS_PER_METRIC) {
                    metrics.put(metric.getName(), metric);
                } else {
                    metrics.put(
                            metric.getName(),
                            new MetricDefinition(
                                    metric.getName(),
                                    metric.getUnit(),
                                    metric.getValues()
                                            .subList(0, Constants.MAX_DATAPOINTS_PER_METRIC)));
                    metricDefinitions.offer(
                            new MetricDefinition(
                                    metric.getName(),
                                    metric.getUnit(),
                                    metric.getValues()
                                            .subList(
                                                    Constants.MAX_DATAPOINTS_PER_METRIC,
                                                    metric.getValues().size())));
                }
            }
            if (!metrics.isEmpty()) {
                nodes.add(buildRootNode(metrics));
            }
            List<String> strings = new ArrayList<>();
            for (RootNode node : nodes) {
                strings.add(node.serialize());
            }
            return strings;
        }
    }

    private RootNode buildRootNode(Map<String, MetricDefinition> metrics) {
        Metadata metadata = rootNode.getAws();
        MetricDirective metricDirective = metadata.getCloudWatchMetrics().get(0);
        Metadata clonedMetadata =
                metadata.withCloudWatchMetrics(Arrays.asList(metricDirective.withMetrics(metrics)));
        return rootNode.withAws(clonedMetadata);
    }

    private boolean anyMetricWithTooManyDataPoints(RootNode node) {
        return node.metrics().values().stream()
                .anyMatch(
                        metric -> metric.getValues().size() > Constants.MAX_DATAPOINTS_PER_METRIC);
    }
}
