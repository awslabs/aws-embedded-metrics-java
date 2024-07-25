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

package software.amazon.cloudwatchlogs.emf.util;

import java.time.Instant;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidNamespaceException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidTimestampException;
import software.amazon.cloudwatchlogs.emf.model.AggregationType;
import software.amazon.cloudwatchlogs.emf.model.Metric;
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;
import software.amazon.cloudwatchlogs.emf.model.Unit;

public class Validator {

    private Validator() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Validates Dimension Set.
     *
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_Dimension.html">CloudWatch
     *     Dimensions</a>
     * @param dimensionName Dimension name
     * @param dimensionValue Dimension value
     * @throws InvalidDimensionException if the dimension name or value is invalid
     */
    public static void validateDimensionSet(String dimensionName, String dimensionValue)
            throws InvalidDimensionException {

        if (dimensionName == null || dimensionName.trim().isEmpty()) {
            throw new InvalidDimensionException("Dimension name cannot be empty");
        }

        if (dimensionValue == null || dimensionValue.trim().isEmpty()) {
            throw new InvalidDimensionException("Dimension value cannot be empty");
        }

        if (dimensionName.length() > Constants.MAX_DIMENSION_NAME_LENGTH) {
            throw new InvalidDimensionException(
                    "Dimension name exceeds maximum length of "
                            + Constants.MAX_DIMENSION_NAME_LENGTH
                            + ": "
                            + dimensionName);
        }

        if (dimensionValue.length() > Constants.MAX_DIMENSION_VALUE_LENGTH) {
            throw new InvalidDimensionException(
                    "Dimension value exceeds maximum length of "
                            + Constants.MAX_DIMENSION_VALUE_LENGTH
                            + ": "
                            + dimensionValue);
        }

        if (!StringUtils.isAsciiPrintable(dimensionName)) {
            throw new InvalidDimensionException(
                    "Dimension name has invalid characters: " + dimensionName);
        }

        if (!StringUtils.isAsciiPrintable(dimensionValue)) {
            throw new InvalidDimensionException(
                    "Dimension value has invalid characters: " + dimensionValue);
        }

        if (dimensionName.startsWith(":")) {
            throw new InvalidDimensionException("Dimension name cannot start with ':'");
        }
    }

    /**
     * Validates Metric.
     *
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html">CloudWatch
     *     Metric</a>
     * @param name Metric name
     * @param value Metric value
     * @param unit Metric unit
     * @param storageResolution Metric resolution
     * @param aggregationType Metric aggregation type
     * @param metricNameAndResolutionMap Map to validate Metric
     * @throws InvalidMetricException if metric is invalid
     */
    public static void validateMetric(
            String name,
            double value,
            Unit unit,
            StorageResolution storageResolution,
            AggregationType aggregationType,
            Map<String, StorageResolution> metricNameAndResolutionMap,
            Map<String, AggregationType> metricNameAndAggregationMap)
            throws InvalidMetricException {

        if (name == null || name.trim().isEmpty()) {
            throw new InvalidMetricException(
                    "Metric name " + name + " must include at least one non-whitespace character");
        }

        if (name.length() > Constants.MAX_METRIC_NAME_LENGTH) {
            throw new InvalidMetricException(
                    "Metric name exceeds maximum length of "
                            + Constants.MAX_METRIC_NAME_LENGTH
                            + ": "
                            + name);
        }

        if (!Double.isFinite(value)) {
            throw new InvalidMetricException("Metric value is not a number");
        }

        if (unit == null) {
            throw new InvalidMetricException("Metric unit cannot be null");
        }

        if (storageResolution == null
                || storageResolution == StorageResolution.UNKNOWN_TO_SDK_VERSION) {
            throw new InvalidMetricException("Metric resolution is invalid");
        }

        if ((metricNameAndResolutionMap.containsKey(name))
                && (!metricNameAndResolutionMap.get(name).equals(storageResolution))) {
            throw new InvalidMetricException(
                    "Resolution for metric "
                            + name
                            + " is already set. A single log event cannot have a metric with two different resolutions.");
        }

        if (aggregationType == null || aggregationType == AggregationType.UNKNOWN_TO_SDK_VERSION) {
            throw new InvalidMetricException("Metric aggregation type is invalid");
        }

        if ((metricNameAndAggregationMap.containsKey(name))
                && (!metricNameAndAggregationMap.get(name).equals(aggregationType))) {
            throw new InvalidMetricException(
                    "Aggregation type for metric "
                            + name
                            + " is already set. A single log event cannot have a metric with two different aggregation types.");
        }
    }

    /**
     * Validates Metric.
     *
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html">CloudWatch
     *     Metric</a>
     * @param name Metric name
     * @param metric Metric to be validated
     * @throws InvalidMetricException if metric is invalid
     */
    public static void validateMetric(String name, Metric metric) throws InvalidMetricException {

        if (name == null || name.trim().isEmpty()) {
            throw new InvalidMetricException(
                    "Metric name " + name + " must include at least one non-whitespace character");
        }

        if (name.length() > Constants.MAX_METRIC_NAME_LENGTH) {
            throw new InvalidMetricException(
                    "Metric name exceeds maximum length of "
                            + Constants.MAX_METRIC_NAME_LENGTH
                            + ": "
                            + name);
        }

        if (metric == null) {
            throw new InvalidMetricException("Metric cannot be null");
        }

        if (!metric.hasValidValues()) {
            throw new InvalidMetricException("Metric has no values");
        }

        if (metric.getUnit() == null) {
            throw new InvalidMetricException("Metric unit cannot be null");
        }

        if (metric.getStorageResolution() == null
                || metric.getStorageResolution() == StorageResolution.UNKNOWN_TO_SDK_VERSION) {
            throw new InvalidMetricException("Metric resolution is invalid");
        }
    }

    /**
     * Validates Namespace.
     *
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Namespace">CloudWatch
     *     Namespace</a>
     * @param namespace Namespace
     * @throws InvalidNamespaceException if the namespace is invalid
     */
    public static void validateNamespace(String namespace) throws InvalidNamespaceException {
        if (namespace == null || namespace.trim().isEmpty()) {
            throw new InvalidNamespaceException(
                    "Namespace must include at least one non-whitespace character");
        }

        if (namespace.length() > Constants.MAX_NAMESPACE_LENGTH) {
            throw new InvalidNamespaceException(
                    "Namespace exceeds maximum length of "
                            + Constants.MAX_NAMESPACE_LENGTH
                            + ": "
                            + namespace);
        }

        if (!namespace.matches(Constants.VALID_NAMESPACE_REGEX)) {
            throw new InvalidNamespaceException(
                    "Namespace contains invalid characters: " + namespace);
        }
    }

    /**
     * Validates Timestamp.
     *
     * @see <a
     *     href="https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#about_timestamp">CloudWatch
     *     Timestamp</a>
     * @param timestamp Timestamp
     * @throws InvalidTimestampException if timestamp is invalid
     */
    public static void validateTimestamp(Instant timestamp) throws InvalidTimestampException {
        if (timestamp == null) {
            throw new InvalidTimestampException("Timestamp cannot be null");
        }

        if (timestamp.isAfter(
                Instant.now().plusSeconds(Constants.MAX_TIMESTAMP_FUTURE_AGE_SECONDS))) {
            throw new InvalidTimestampException(
                    "Timestamp cannot be more than "
                            + Constants.MAX_TIMESTAMP_FUTURE_AGE_SECONDS
                            + " seconds in the future");
        }

        if (timestamp.isBefore(
                Instant.now().minusSeconds(Constants.MAX_TIMESTAMP_PAST_AGE_SECONDS))) {
            throw new InvalidTimestampException(
                    "Timestamp cannot be more than "
                            + Constants.MAX_TIMESTAMP_PAST_AGE_SECONDS
                            + " seconds in the past");
        }
    }
}
