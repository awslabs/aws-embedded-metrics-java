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
import java.util.List;
import java.util.Queue;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;

/** Represents the Histogram of the EMF schema. */
public class HistogramMetric extends Metric<Histogram> {

    HistogramMetric(
            Unit unit,
            StorageResolution storageResolution,
            List<Double> values,
            List<Integer> counts)
            throws IllegalArgumentException {
        this(unit, storageResolution, new Histogram(values, counts));
    }

    protected HistogramMetric(
            String name, Unit unit, StorageResolution storageResolution, Histogram histogram) {
        this.unit = unit;
        this.storageResolution = storageResolution;
        this.values = histogram;
        this.name = name;
    }

    HistogramMetric(Unit unit, StorageResolution storageResolution, Histogram histogram) {
        this.unit = unit;
        this.storageResolution = storageResolution;
        this.values = histogram;
    }

    @Override
    protected Queue<Metric<Histogram>> serialize() throws InvalidMetricException {
        // Histograms will be rejected from CWL if they have more than
        // Constants.MAX_DATAPOINTS_PER_METRIC number of bins. Unlike MetricDefinition histograms
        // cannot be broken into multiple messages therefore an error is raised to let users know
        // their message won't be sent otherwise only this metric will be sent
        if (isOversized()) {
            throw new InvalidMetricException(
                    String.format(
                            "Histogram metric, %s, has %d values which exceeds the maximum amount "
                                    + "of bins allowed, %d, and Histograms cannot be broken into "
                                    + "multiple metrics therefore it will not be published",
                            name, values.values.size(), Constants.MAX_DATAPOINTS_PER_METRIC));
        }
        Queue<Metric<Histogram>> metrics = new LinkedList<>();
        metrics.offer(this);
        return metrics;
    }

    @Override
    protected boolean isOversized() {
        return values.values.size() > Constants.MAX_DATAPOINTS_PER_METRIC;
    }

    @Override
    public boolean hasValidValues() {
        return values != null && values.count > 0 && !isOversized();
    }

    public static HistogramMetricBuilder builder() {
        return new HistogramMetricBuilder();
    }

    public static class HistogramMetricBuilder
            extends Metric.MetricBuilder<Histogram, HistogramMetricBuilder> {

        @Override
        protected HistogramMetricBuilder getThis() {
            return this;
        }

        public HistogramMetricBuilder() {
            this.values = new Histogram();
        }

        @Override
        public Histogram getValues() {
            rwl.readLock().lock();
            try {
                return values.reduce();
            } finally {
                rwl.readLock().unlock();
            }
        }

        @Override
        public HistogramMetricBuilder addValue(double value) {
            rwl.readLock().lock();
            try {
                values.addValue(value);
                return this;
            } finally {
                rwl.readLock().unlock();
            }
        }

        @Override
        public HistogramMetric build() {
            rwl.writeLock().lock();
            try {
                values.reduce();
                if (name == null) {
                    return new HistogramMetric(unit, storageResolution, values);
                }
                return new HistogramMetric(name, unit, storageResolution, values);
            } finally {
                rwl.writeLock().unlock();
            }
        }
    }
}
