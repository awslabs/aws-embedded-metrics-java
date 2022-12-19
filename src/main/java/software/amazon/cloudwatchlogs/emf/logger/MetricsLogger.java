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

package software.amazon.cloudwatchlogs.emf.logger;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidTimestampException;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import software.amazon.cloudwatchlogs.emf.sinks.ISink;

/**
 * A metrics logger. Use this interface to publish logs to CloudWatch Logs and extract metrics to
 * CloudWatch Metrics asynchronously.
 */
@Slf4j
public class MetricsLogger {
    private MetricsContext context;
    private CompletableFuture<Environment> environmentFuture;
    private EnvironmentProvider environmentProvider;
    /**
     * This lock is used to create an internal sync context for flush() method in multi-threaded
     * situations. Flush() acquires write lock, other methods (accessing mutable shared data with
     * flush()) acquires read lock. This makes sure flush() is executed exclusively, while other
     * methods can be executed concurrently.
     */
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();

    @Getter @Setter private boolean flushPreserveDimensions = true;

    public MetricsLogger() {
        this(new EnvironmentProvider());
    }

    public MetricsLogger(Environment environment) {
        context = new MetricsContext();
        environmentFuture = CompletableFuture.completedFuture(environment);
        environmentProvider = null; // TODO: should do some refactoring here
    }

    public MetricsLogger(EnvironmentProvider environmentProvider) {
        this(environmentProvider, new MetricsContext());
    }

    public MetricsLogger(EnvironmentProvider environmentProvider, MetricsContext metricsContext) {
        context = metricsContext;
        environmentFuture = environmentProvider.resolveEnvironment();
        this.environmentProvider = environmentProvider;
    }

    /**
     * Flushes the current context state to the configured sink. TODO: Support flush asynchronously
     */
    public void flush() {
        Environment environment;
        try {
            environment = environmentFuture.join();
        } catch (Exception ex) {
            log.info("Failed to resolve environment. Fallback to default environment: ", ex);
            environment = environmentProvider.getDefaultEnvironment();
        }

        rwl.writeLock().lock();
        try {
            ISink sink = environment.getSink();
            configureContextForEnvironment(context, environment);
            sink.accept(context);
            context = context.createCopyWithContext(flushPreserveDimensions);
        } finally {
            rwl.writeLock().unlock();
        }
    }

    /**
     * Set a property on the published metrics. This is stored in the emitted log data, and you are
     * not charged for this data by CloudWatch Metrics. These values can be values that are useful
     * for searching on, but have too high cardinality to emit as dimensions to CloudWatch Metrics.
     *
     * @param key Property name
     * @param value Property value
     * @return the current logger
     */
    public MetricsLogger putProperty(String key, Object value) {
        return applyReadLock(
                () -> {
                    this.context.putProperty(key, value);
                    return this;
                });
    }

    /**
     * Adds a dimension. This is generally a low cardinality key-value pair that is part of the
     * metric identity. CloudWatch treats each unique combination of dimensions as a separate
     * metric, even if the metrics have the same metric name.
     *
     * @param dimensions the DimensionSet to add
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Dimension">CloudWatch
     *     Dimensions</a>
     * @return the current logger
     */
    public MetricsLogger putDimensions(DimensionSet dimensions) {
        return applyReadLock(
                () -> {
                    context.putDimension(dimensions);
                    return this;
                });
    }

    /**
     * Overwrite all dimensions on this MetricsLogger instance.
     *
     * @param dimensionSets the dimensionSets to set
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Dimension">CloudWatch
     *     Dimensions</a>
     * @return the current logger
     */
    public MetricsLogger setDimensions(DimensionSet... dimensionSets) {
        return applyReadLock(
                () -> {
                    context.setDimensions(dimensionSets);
                    return this;
                });
    }

    /**
     * Overwrite custom dimensions on this MetricsLogger instance, with an option to preserve
     * default dimensions.
     *
     * @param useDefault indicates whether default dimensions should be used
     * @param dimensionSets the dimensionSets to set
     * @return the current logger
     */
    public MetricsLogger setDimensions(boolean useDefault, DimensionSet... dimensionSets) {
        return applyReadLock(
                () -> {
                    context.setDimensions(useDefault, dimensionSets);
                    return this;
                });
    }

    /**
     * Clear all custom dimensions on this MetricsLogger instance. Whether default dimensions should
     * be used can be configured by the input parameter.
     *
     * @param useDefault indicates whether default dimensions should be used
     * @return the current logger
     */
    public MetricsLogger resetDimensions(boolean useDefault) {
        return applyReadLock(
                () -> {
                    context.resetDimensions(useDefault);
                    return this;
                });
    }

    /**
     * Put a metric value. This value will be emitted to CloudWatch Metrics asynchronously and does
     * not contribute to your account TPS limits. The value will also be available in your
     * CloudWatch Logs
     *
     * @param key is the name of the metric
     * @param value is the value of the metric
     * @param unit is the unit of the metric value
     * @param storageResolution is the resolution of the metric
     * @return the current logger
     * @throws InvalidMetricException if the metric is invalid
     */
    public MetricsLogger putMetric(
            String key, double value, Unit unit, StorageResolution storageResolution)
            throws InvalidMetricException {
        rwl.readLock().lock();
        try {
            this.context.putMetric(key, value, unit, storageResolution);
            return this;
        } finally {
            rwl.readLock().unlock();
        }
    }

    /**
     * Put a metric value. This value will be emitted to CloudWatch Metrics asynchronously and does
     * not contribute to your account TPS limits. The value will also be available in your
     * CloudWatch Logs
     *
     * @param key is the name of the metric
     * @param value is the value of the metric
     * @param storageResolution is the resolution of the metric
     * @return the current logger
     * @throws InvalidMetricException if the metric is invalid
     */
    public MetricsLogger putMetric(String key, double value, StorageResolution storageResolution)
            throws InvalidMetricException {
        this.putMetric(key, value, Unit.NONE, storageResolution);
        return this;
    }

    /**
     * Put a metric value. This value will be emitted to CloudWatch Metrics asynchronously and does
     * not contribute to your account TPS limits. The value will also be available in your
     * CloudWatch Logs
     *
     * @param key is the name of the metric
     * @param value is the value of the metric
     * @param unit is the unit of the metric value
     * @return the current logger
     * @throws InvalidMetricException if the metric is invalid
     */
    public MetricsLogger putMetric(String key, double value, Unit unit)
            throws InvalidMetricException {
        this.putMetric(key, value, unit, StorageResolution.STANDARD);
        return this;
    }

    /**
     * Put a metric value. This value will be emitted to CloudWatch Metrics asynchronously and does
     * not contribute to your account TPS limits. The value will also be available in your
     * CloudWatch Logs
     *
     * @param key the name of the metric
     * @param value the value of the metric
     * @return the current logger
     * @throws InvalidMetricException if the metric is invalid
     */
    public MetricsLogger putMetric(String key, double value) throws InvalidMetricException {
        this.putMetric(key, value, Unit.NONE, StorageResolution.STANDARD);
        return this;
    }

    /**
     * Add a custom key-value pair to the Metadata object.
     *
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Specification.html#CloudWatch_Embedded_Metric_Format_Specification_structure_metadata">CloudWatch
     *     Metadata</a>
     * @param key the name of the key
     * @param value the value associated with the key
     * @return the current logger
     */
    public MetricsLogger putMetadata(String key, Object value) {
        return applyReadLock(
                () -> {
                    this.context.putMetadata(key, value);
                    return this;
                });
    }

    /**
     * Set the CloudWatch namespace that metrics should be published to.
     *
     * @param namespace the namespace of the logs
     * @return the current logger
     * @throws InvalidNamespaceException if the namespace is invalid
     */
    public MetricsLogger setNamespace(String namespace) throws InvalidNamespaceException {
        this.context.setNamespace(namespace);
        return this;
    }

    /**
     * Set the timestamp to be used for metrics.
     *
     * @param timestamp value of timestamp to be set
     * @return the current logger
     * @throws InvalidTimestampException if the timestamp is invalid
     */
    public MetricsLogger setTimestamp(Instant timestamp) throws InvalidTimestampException {
        this.context.setTimestamp(timestamp);
        return this;
    }

    private void configureContextForEnvironment(MetricsContext context, Environment environment) {
        if (context.hasDefaultDimensions()) {
            return;
        }
        DimensionSet defaultDimension = new DimensionSet();
        setDefaultDimension(defaultDimension, "LogGroup", environment.getLogGroupName());
        setDefaultDimension(defaultDimension, "ServiceName", environment.getName());
        setDefaultDimension(defaultDimension, "ServiceType", environment.getType());
        context.setDefaultDimensions(defaultDimension);
        environment.configureContext(context);
    }

    private void setDefaultDimension(DimensionSet defaultDimension, String dimKey, String dimVal) {
        try {
            defaultDimension.addDimension(dimKey, dimVal);
        } catch (InvalidDimensionException | DimensionSetExceededException ignored) {
        }
    }

    private MetricsLogger applyReadLock(Supplier<MetricsLogger> any) {
        rwl.readLock().lock();
        try {
            return any.get();
        } finally {
            rwl.readLock().unlock();
        }
    }
}
