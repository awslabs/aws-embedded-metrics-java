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

import com.fasterxml.jackson.annotation.JsonProperty;

class Statistics {
    Statistics(double max, double min, int count, double sum) throws IllegalArgumentException {
        this.max = max;
        this.min = min;
        this.count = count;
        this.sum = sum;
        if (!isValid()) {
            throw new IllegalArgumentException(
                    "This is an impossible statistic set, there is no set of values that can create these statistics.");
        }
    }

    Statistics() {
        count = 0;
        sum = 0.;
    };

    @JsonProperty("Max")
    public Double max = Double.MIN_VALUE;

    @JsonProperty("Min")
    public Double min = Double.MAX_VALUE;

    @JsonProperty("Count")
    public int count;

    @JsonProperty("Sum")
    public Double sum;

    void addValue(double value) {
        count++;
        sum += value;
        if (value > max) {
            max = value;
        }
        if (value < min) {
            min = value;
        }
    }

    /** @returns true if this object represents a possible non-empy set of real values. */
    boolean isValid() {
        return !(max < min
                // Statistic set must not be empty or have a negative count
                || (count <= 0)
                // The max and min must be the same if there is only one value
                || (count == 1 && Math.abs(max - min) > 1e-5)
                // the sum must be less than or equal to the greatest possible value that could be
                // created with this max, min and count
                || min + max * (count - 1) < sum
                // the sum must be greater than or equal to the smallest possible value that could
                // be created with this max, min and count
                || max + min * (count - 1) > sum);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Statistics that = (Statistics) o;
        return count == that.count
                && that.sum.equals(sum)
                && that.max.equals(max)
                && that.min.equals(min);
    }

    @Override
    public int hashCode() {
        return count + sum.hashCode() + max.hashCode() + min.hashCode();
    }
}
