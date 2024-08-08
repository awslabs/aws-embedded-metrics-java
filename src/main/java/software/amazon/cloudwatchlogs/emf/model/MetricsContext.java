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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidTimestampException;
import software.amazon.cloudwatchlogs.emf.util.Validator;

/** Stores metrics and their associated properties and dimensions. */
public class MetricsContext {

    @Getter private final RootNode rootNode;

    private MetricDirective metricDirective;
    private final Map<String, StorageResolution> metricNameAndResolutionMap =
            new ConcurrentHashMap<>();
    private final Map<String, AggregationType> metricNameAndAggregationMap =
            new ConcurrentHashMap<>();

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
            DimensionSet defaultDimensionSet)
            throws InvalidNamespaceException {
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
     * @throws InvalidNamespaceException if the namespace is invalid
     */
    public void setNamespace(String namespace) throws InvalidNamespaceException {
        Validator.validateNamespace(namespace);
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
        return !getDefaultDimensions().getDimensionKeys().isEmpty();
    }

    /**
     * Add a metric measurement to the context. Multiple calls using the same key will be stored as
     * an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Latency", 100, Unit.MILLISECONDS, StorageResolution.HIGH, AggregationType.LIST)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param unit The unit of the metric
     * @param storageResolution The resolution of the metric
     * @param aggregationType The aggregation type of the metric
     * @throws InvalidMetricException if the metric is invalid
     */
    public void putMetric(
            String key,
            double value,
            Unit unit,
            StorageResolution storageResolution,
            AggregationType aggregationType)
            throws InvalidMetricException {
        Validator.validateMetric(
                key,
                value,
                unit,
                storageResolution,
                aggregationType,
                metricNameAndResolutionMap,
                metricNameAndAggregationMap);
        metricDirective.putMetric(key, value, unit, storageResolution, aggregationType);
        metricNameAndResolutionMap.put(key, storageResolution);
        metricNameAndAggregationMap.put(key, aggregationType);
    }

    /**
     * Add a metric measurement to the context. Multiple calls using the same key will be stored as
     * an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Latency", 100, Unit.MILLISECONDS, StorageResolution.HIGH)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param unit The unit of the metric
     * @param storageResolution The resolution of the metric
     * @throws InvalidMetricException if the metric is invalid
     */
    public void putMetric(String key, double value, Unit unit, StorageResolution storageResolution)
            throws InvalidMetricException {
        putMetric(key, value, unit, storageResolution, AggregationType.LIST);
    }

    /**
     * Add a metric measurement to the context with a storage resolution but without a unit.
     * Multiple calls using the same key will be stored as an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Latency", 100, StorageResolution.HIGH)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param storageResolution The resolution of the metric
     * @throws InvalidMetricException if the metric is invalid
     */
    public void putMetric(String key, double value, StorageResolution storageResolution)
            throws InvalidMetricException {
        putMetric(key, value, Unit.NONE, storageResolution, AggregationType.LIST);
    }

    /**
     * Add a metric measurement to the context without a storage resolution. Multiple calls using
     * the same key will be stored as an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Latency", 100, Unit.MILLISECONDS)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param unit The unit of the metric
     * @throws InvalidMetricException if the metric is invalid
     */
    public void putMetric(String key, double value, Unit unit) throws InvalidMetricException {
        putMetric(key, value, unit, StorageResolution.STANDARD, AggregationType.LIST);
    }

    /**
     * Add a metric measurement to the context without a unit Multiple calls using the same key will
     * be stored as an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Count", 10, AggregationType.LIST)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param aggregationType The aggregation type of the metric
     * @throws InvalidMetricException if the metric is invalid
     */
    public void putMetric(String key, double value, AggregationType aggregationType)
            throws InvalidMetricException {
        putMetric(key, value, Unit.NONE, StorageResolution.STANDARD, aggregationType);
    }

    /**
     * Add a metric measurement to the context with a storage resolution but without a unit.
     * Multiple calls using the same key will be stored as an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Latency", 100, StorageResolution.HIGH, AggregationType.LIST)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param storageResolution The resolution of the metric
     * @param aggregationType The aggregation type of the metric
     * @throws InvalidMetricException if the metric is invalid
     */
    public void putMetric(
            String key,
            double value,
            StorageResolution storageResolution,
            AggregationType aggregationType)
            throws InvalidMetricException {
        putMetric(key, value, Unit.NONE, storageResolution, aggregationType);
    }

    /**
     * Add a metric measurement to the context without a storage resolution. Multiple calls using
     * the same key will be stored as an array of scalar values.
     *
     * <pre>{@code
     * metricContext.putMetric("Latency", 100, Unit.MILLISECONDS, AggregationType.LIST)
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @param unit The unit of the metric
     * @param aggregationType The aggregation type of the metric
     * @throws InvalidMetricException if the metric is invalid
     */
    public void putMetric(String key, double value, Unit unit, AggregationType aggregationType)
            throws InvalidMetricException {
        putMetric(key, value, unit, StorageResolution.STANDARD, aggregationType);
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
     * @throws InvalidMetricException if the metric is invalid
     */
    public void putMetric(String key, double value) throws InvalidMetricException {
        putMetric(key, value, Unit.NONE, StorageResolution.STANDARD, AggregationType.LIST);
    }

    /**
     * Set a metric measurement to the context overwriting any existing metric that may be
     * associated with that key.
     *
     * <pre>{@code
     * metricContext.setMetric("Latency", StatisticSet.builer().addValue(10).addValue(100).build())
     * }</pre>
     *
     * @param key Name of the metric
     * @param value Value of the metric
     * @throws InvalidMetricException if the metric is invalid
     */
    public void setMetric(String key, Metric value) throws InvalidMetricException {
        Validator.validateMetric(key, value);
        metricDirective.setMetric(key, value);
        metricNameAndResolutionMap.put(key, value.storageResolution);
        metricNameAndAggregationMap.put(key, AggregationType.STATISTIC_SET);
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
     * @throws InvalidDimensionException if the dimension is invalid
     * @throws DimensionSetExceededException if the number of dimensions exceeds the limit
     */
    public void putDimension(String dimension, String value)
            throws InvalidDimensionException, DimensionSetExceededException {
        metricDirective.putDimensionSet(DimensionSet.of(dimension, value));
    }

    /**
     * Get list of all dimensions including default dimensions
     *
     * @return the list of dimensions that has been added, including default dimensions.
     * @throws DimensionSetExceededException if the number of dimensions exceeds the limit
     */
    public List<DimensionSet> getDimensions() throws DimensionSetExceededException {
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
     * @throws InvalidTimestampException if the timestamp is invalid
     */
    public void setTimestamp(Instant timestamp) throws InvalidTimestampException {
        Validator.validateTimestamp(timestamp);
        rootNode.getAws().setTimestamp(timestamp);
    }

    /**
     * Create a copy of the context
     *
     * @param preserveDimensions indicates whether default dimensions should be preserved
     * @return Creates an independently flushable context
     */
    public MetricsContext createCopyWithContext(boolean preserveDimensions) {
        return new MetricsContext(metricDirective.copyWithoutMetrics(preserveDimensions));
    }

    /**
     * Serialize the metrics in this context to strings. The EMF backend requires no more than 100
     * metrics in one log event. If there are more than 100 metrics, we split the metrics into
     * multiple log events.
     *
     * <p>If a metric has more than 100 data points, we also split the metric.
     *
     * @return the serialized strings.
     * @throws JsonProcessingException if there's any object that cannot be serialized
     */
    public List<String> serialize() throws JsonProcessingException, InvalidMetricException {
        if (rootNode.metrics().size() <= Constants.MAX_METRICS_PER_EVENT
                && !anyMetricWithTooManyDataPoints(rootNode)) {
            return Arrays.asList(this.rootNode.serialize());
        } else {
            List<RootNode> nodes = new ArrayList<>();
            Map<String, Metric<?>> metrics = new HashMap<>();
            ArrayList<Queue<Metric<?>>> remainingMetrics = new ArrayList<>();
            PriorityQueue<Queue<Metric<?>>> metricQueue =
                    new PriorityQueue<>((x, y) -> Integer.compare(x.size(), y.size()));

            for (Metric metric : rootNode.metrics().values()) {
                metricQueue.offer(metric.serialize());
            }

            // Split metrics into batches of 100 (max allowed by EMF backend
            while (!metricQueue.isEmpty() || !remainingMetrics.isEmpty()) {
                if (metrics.size() == Constants.MAX_METRICS_PER_EVENT
                        || metricQueue.isEmpty()
                        || metrics.containsKey(metricQueue.peek().peek().getName())) {
                    nodes.add(buildRootNode(metrics));
                    metrics = new HashMap<>();
                    metricQueue.addAll(remainingMetrics);
                    remainingMetrics.clear();
                }

                Queue<Metric<?>> serializedMetrics = metricQueue.poll();
                Metric firstBatch = serializedMetrics.poll();

                metrics.put(firstBatch.getName(), firstBatch);

                if (!serializedMetrics.isEmpty()) {
                    remainingMetrics.add(serializedMetrics);
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

    private RootNode buildRootNode(Map<String, Metric<?>> metrics) {
        Metadata metadata = rootNode.getAws();
        MetricDirective md = metadata.getCloudWatchMetrics().get(0);
        Metadata clonedMetadata =
                metadata.withCloudWatchMetrics(Arrays.asList(md.withMetrics(metrics)));
        return rootNode.withAws(clonedMetadata);
    }

    private boolean anyMetricWithTooManyDataPoints(RootNode node) {
        return node.metrics().values().stream().anyMatch(metric -> metric.isOversized());
    }
}
