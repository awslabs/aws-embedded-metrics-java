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

import java.util.ArrayList;
import java.util.List;
import lombok.NonNull;

/** Represents the MetricDefinition of the EMF schema. */
public class MetricDefinition extends Metric<List<Double>> {

    private MetricDefinition(
            @NonNull String name,
            Unit unit,
            StorageResolution storageResolution,
            List<Double> values) {
        this.unit = unit;
        this.storageResolution = storageResolution;
        this.values = values;
        this.name = name;
    }

    MetricDefinition(Unit unit, StorageResolution storageResolution, List<Double> values) {
        this.unit = unit;
        this.storageResolution = storageResolution;
        this.values = values;
    }

    @Override
    protected Metric getMetricValuesUnderSize(int size) {
        List<Double> subList = values.subList(0, Math.min(values.size(), size));
        MetricDefinition metric =
                MetricDefinition.builder()
                        .unit(unit)
                        .storageResolution(storageResolution)
                        .values(subList)
                        .build();
        metric.setName(name);
        return metric;
    }

    @Override
    protected Metric getMetricValuesOverSize(int size) {
        if (size > values.size()) {
            return null;
        }
        List<Double> subList = values.subList(size, values.size());
        MetricDefinition metric =
                MetricDefinition.builder()
                        .name(name)
                        .unit(unit)
                        .storageResolution(storageResolution)
                        .values(subList)
                        .build();
        return metric;
    }

    public static MetricDefinitionBuilder builder() {
        return new MetricDefinitionBuilder();
    }

    /**
     * @return the values of this metric, simplified to a double instead of a list if there is only
     *     one value
     */
    @Override
    protected Object getFormattedValues() {
        return values.size() == 1 ? values.get(0) : values;
    }

    public static class MetricDefinitionBuilder
            extends Metric.MetricBuilder<List<Double>, MetricDefinitionBuilder> {

        @Override
        protected MetricDefinitionBuilder getThis() {
            return this;
        }

        public MetricDefinitionBuilder() {
            this.values = new ArrayList<>();
        }

        @Override
        public MetricDefinitionBuilder addValue(double value) {
            this.values.add(value);
            return this;
        }

        public MetricDefinitionBuilder values(@NonNull List<Double> values) {
            this.values = values;
            return this;
        }

        @Override
        public MetricDefinition build() {
            if (name == null) {
                return new MetricDefinition(unit, storageResolution, values);
            }
            return new MetricDefinition(name, unit, storageResolution, values);
        }
    }
}
