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
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import software.amazon.cloudwatchlogs.emf.serializers.StorageResolutionFilter;
import software.amazon.cloudwatchlogs.emf.serializers.StorageResolutionSerializer;
import software.amazon.cloudwatchlogs.emf.serializers.UnitDeserializer;
import software.amazon.cloudwatchlogs.emf.serializers.UnitSerializer;

/** Abstract immutable (except for name) class that all Metrics are based on. */
@Getter
public abstract class Metric<V> {
    @JsonProperty("Name")
    @Setter(AccessLevel.PROTECTED)
    @NonNull
    protected String name;

    @JsonProperty("Unit")
    @JsonSerialize(using = UnitSerializer.class)
    @JsonDeserialize(using = UnitDeserializer.class)
    protected Unit unit;

    @JsonProperty("StorageResolution")
    @JsonInclude(
            value = JsonInclude.Include.CUSTOM,
            valueFilter =
                    StorageResolutionFilter.class) // Do not serialize when valueFilter is true
    @JsonSerialize(using = StorageResolutionSerializer.class)
    protected StorageResolution storageResolution;

    @JsonIgnore @Getter protected V values;

    /** @return the values of this metric formatted to be flushed */
    protected Object getFormattedValues() {
        return this.getValues();
    }

    /**
     * Creates a Metric with the first {@code size} values of the current metric
     *
     * @param size the maximum size of the returned metric's values
     * @return a Metric with the first {@code size} values of the current metric.
     */
    protected abstract Metric getMetricValuesUnderSize(int size);

    /**
     * Creates a Metric all metrics after the first {@code size} values of the current metric. If
     * there are less than {@code size} values, null is returned.
     *
     * @param size the maximum size of the returned metric's values
     * @return a Metric with the all metrics after the first {@code size} values of the current
     *     metric. If there are less than {@code size} values, null is returned.
     */
    protected abstract Metric getMetricValuesOverSize(int size);

    public abstract static class MetricBuilder<V, T extends MetricBuilder<V, T>> extends Metric<V> {

        protected abstract T getThis();

        /**
         * Adds a value to the metric.
         *
         * @param value the value to be added to this metric
         */
        abstract T addValue(double value);

        /**
         * Builds the metric.
         *
         * @return the built metric
         */
        abstract Metric build();

        protected T name(@NonNull String name) {
            this.name = name;
            return getThis();
        }

        public T unit(Unit unit) {
            this.unit = unit;
            return getThis();
        }

        public T storageResolution(StorageResolution storageResolution) {
            this.storageResolution = storageResolution;
            return getThis();
        }

        protected Metric getMetricValuesOverSize(int size) {
            return build().getMetricValuesOverSize(size);
        }

        protected Metric getMetricValuesUnderSize(int size) {
            return build().getMetricValuesUnderSize(size);
        }

        protected Object getFormattedValues() {
            return build().getFormattedValues();
        }
    }
}
