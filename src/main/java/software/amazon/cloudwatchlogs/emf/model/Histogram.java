/*   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;

/** Histogram metric type */
class Histogram extends Statistics {
    Histogram(List<Double> values, List<Integer> counts) throws IllegalArgumentException {
        if (counts.size() != values.size()) {
            throw new IllegalArgumentException("Counts and values must have the same size");
        }

        if (values.stream().anyMatch(n -> n == null) || counts.stream().anyMatch(n -> n == null)) {
            throw new IllegalArgumentException("Values and counts cannot contain null values");
        }

        if (!validSize(counts.size())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Histogram provided with %d bins but CloudWatch will drop Histograms with more than %d bins",
                            counts.size(), Constants.MAX_DATAPOINTS_PER_METRIC));
        }

        this.max = Collections.max(values);
        this.min = Collections.min(values);
        this.count = counts.stream().mapToInt(Integer::intValue).sum();
        this.sum = 0d;
        for (int i = 0; i < counts.size(); i++) {
            this.sum += values.get(i) * counts.get(i);
        }
        this.counts = counts;
        this.values = values;
    }

    Histogram() {
        count = 0;
        sum = 0.;
        values = new ArrayList<>();
        counts = new ArrayList<>();
    }

    @JsonProperty("Values")
    public List<Double> values;

    @JsonProperty("Counts")
    public List<Integer> counts;

    @JsonIgnore private boolean reduced = false;

    @JsonIgnore private static final double EPSILON = 0.1;
    @JsonIgnore private static final double BIN_SIZE = Math.log(1 + EPSILON);
    @JsonIgnore private final Map<Double, Integer> buckets = new HashMap<>();

    /**
     * @param value the value to add to the histogram
     * @throws InvalidMetricException if adding this value would increase the number of bins in the
     *     histogram to more than {@value Constants#MAX_DATAPOINTS_PER_METRIC}
     * @see Constants#MAX_DATAPOINTS_PER_METRIC
     */
    @Override
    void addValue(double value) throws InvalidMetricException {
        reduced = false;
        super.addValue(value);

        double bucket = getBucket(value);
        if (!buckets.containsKey(bucket) && !validSize(counts.size() + 1)) {
            throw new InvalidMetricException(
                    String.format(
                            "Adding this value increases the number of bins in this histogram to %d"
                                    + ", CloudWatch will drop any Histogram metrics with more than %d bins",
                            buckets.size() + 1, Constants.MAX_DATAPOINTS_PER_METRIC));
        }
        // Add the value to the appropriate bucket (or create a new bucket if necessary)
        buckets.compute(
                bucket,
                (k, v) -> {
                    if (v == null) {
                        return 1;
                    } else {
                        return v + 1;
                    }
                });
    }

    /**
     * Updates the Values and Counts lists to represent the buckets of this histogram.
     *
     * @return the reduced histogram
     */
    Histogram reduce() {
        if (reduced) {
            return this;
        }

        this.values = new ArrayList<>(buckets.size());
        this.counts = new ArrayList<>(buckets.size());

        for (Map.Entry<Double, Integer> entry : buckets.entrySet()) {
            this.values.add(entry.getKey());
            this.counts.add(entry.getValue());
        }

        reduced = true;
        return this;
    }

    /**
     * Gets the value of the bucket for the given value.
     *
     * @param value the value to find the closest bucket for
     * @return the value of the bucket the given value goes in
     */
    private static double getBucket(double value) {
        short index = (short) Math.floor(Math.log(value) / BIN_SIZE);
        return Math.exp((index + 0.5) * BIN_SIZE);
    }

    private boolean validSize(int size) {
        return size <= Constants.MAX_DATAPOINTS_PER_METRIC;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Histogram that = (Histogram) o;
        return count == that.count
                && that.sum.equals(sum)
                && that.max.equals(max)
                && that.min.equals(min)
                && buckets.equals(that.buckets);
    }

    @Override
    public int hashCode() {
        return super.hashCode() + buckets.hashCode();
    }
}
