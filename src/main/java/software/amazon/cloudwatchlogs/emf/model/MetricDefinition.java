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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import lombok.NonNull;
import software.amazon.cloudwatchlogs.emf.Constants;

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
    protected Queue<Metric<List<Double>>> serialize() {
        Queue<Metric<List<Double>>> metrics = new LinkedList<>();
        MetricDefinition metric = this;
        while (metric != null) {
            metrics.add(metric.getFirstMetricBatch(Constants.MAX_DATAPOINTS_PER_METRIC));
            metric = metric.getRemainingMetricBatch(Constants.MAX_DATAPOINTS_PER_METRIC);
        }

        return metrics;
    }

    private MetricDefinition getFirstMetricBatch(int batchSize) {
        List<Double> subList = values.subList(0, Math.min(values.size(), batchSize));
        MetricDefinition metric =
                MetricDefinition.builder()
                        .unit(unit)
                        .storageResolution(storageResolution)
                        .values(subList)
                        .build();
        metric.setName(name);
        return metric;
    }

    private MetricDefinition getRemainingMetricBatch(int batchSize) {
        if (batchSize >= values.size()) {
            return null;
        }
        List<Double> subList = values.subList(batchSize, values.size());
        MetricDefinition metric =
                MetricDefinition.builder()
                        .name(name)
                        .unit(unit)
                        .storageResolution(storageResolution)
                        .values(subList)
                        .build();
        return metric;
    }

    protected boolean isOversized() {
        return values.size() > Constants.MAX_DATAPOINTS_PER_METRIC;
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

    @Override
    public boolean hasValidValues() {
        return values != null && !values.isEmpty();
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
