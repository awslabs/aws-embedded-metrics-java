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
import java.util.Queue;
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
    protected Unit unit = Unit.NONE;

    @JsonProperty("StorageResolution")
    @JsonInclude(
            value = JsonInclude.Include.CUSTOM,
            valueFilter =
                    StorageResolutionFilter.class) // Do not serialize when valueFilter is true
    @JsonSerialize(using = StorageResolutionSerializer.class)
    protected StorageResolution storageResolution = StorageResolution.STANDARD;

    @JsonIgnore
    @Getter(AccessLevel.PROTECTED)
    protected V values;

    /** @return the values of this metric formatted to be flushed */
    protected Object getFormattedValues() {
        return this.getValues();
    }

    /** @return true if the values of this metric are valid, false otherwise. */
    public abstract boolean hasValidValues();

    /** @return true if the values of this metric are oversized for CloudWatch Logs */
    protected abstract boolean isOversized();

    /**
     * Creates a list of new Metrics based off the values in this metric split in so that they are
     * small enough that CWL will not drop the message values
     *
     * @return a list of metrics based off of the values of this metric that aren't too large for
     *     CWL
     */
    protected abstract Queue<Metric<V>> serialize();

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
        abstract Metric<V> build();

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

        @Override
        public boolean hasValidValues() {
            return build().hasValidValues();
        }

        @Override
        protected Queue<Metric<V>> serialize() {
            return build().serialize();
        }

        @Override
        protected Object getFormattedValues() {
            return build().getFormattedValues();
        }

        @Override
        protected boolean isOversized() {
            return build().isOversized();
        }
    }
}
