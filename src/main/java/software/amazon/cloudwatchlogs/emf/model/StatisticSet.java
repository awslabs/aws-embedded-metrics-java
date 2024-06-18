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

import java.util.LinkedList;
import lombok.NonNull;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;

/** Represents the StatisticSet of the EMF schema. */
public class StatisticSet extends Metric<Statistics> {

    StatisticSet(
            Unit unit,
            StorageResolution storageResolution,
            double max,
            double min,
            int count,
            double sum) {
        this(unit, storageResolution, new Statistics(max, min, count, sum));
    }

    protected StatisticSet(
            @NonNull String name,
            Unit unit,
            StorageResolution storageResolution,
            Statistics statistics) {
        this.unit = unit;
        this.storageResolution = storageResolution;
        this.values = statistics;
        this.name = name;
    }

    StatisticSet(Unit unit, StorageResolution storageResolution, Statistics statistics) {
        this.unit = unit;
        this.storageResolution = storageResolution;
        this.values = statistics;
    }

    @Override
    protected LinkedList<Metric> serialize() throws InvalidMetricException {
        // A statistic set is a complete metric that cannot be broken into smaller pieces therefore
        // this metric will be the only one in the returned list
        LinkedList<Metric> queue = new LinkedList<>();
        queue.add(this);

        return queue;
    }

    protected boolean isOversized() {
        return false; // StatisticSets cannot be oversized according to CWL
    }

    @Override
    public boolean hasValidValues() {
        return values != null && values.isValid();
    }

    public static StatisticSetBuilder builder() {
        return new StatisticSetBuilder();
    }

    public static class StatisticSetBuilder
            extends Metric.MetricBuilder<Statistics, StatisticSetBuilder> {

        @Override
        protected StatisticSetBuilder getThis() {
            return this;
        }

        public StatisticSetBuilder() {
            values = new Statistics();
        }

        @Override
        public StatisticSetBuilder addValue(double value) {
            this.values.addValue(value);
            return this;
        }

        public StatisticSetBuilder values(@NonNull Statistics values) {
            this.values = values;
            return this;
        }

        @Override
        public StatisticSet build() {
            if (name == null) {
                return new StatisticSet(unit, storageResolution, values);
            }
            return new StatisticSet(name, unit, storageResolution, values);
        }
    }
}
