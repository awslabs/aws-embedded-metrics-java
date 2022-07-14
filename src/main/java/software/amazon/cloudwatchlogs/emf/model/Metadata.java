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

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.With;
import software.amazon.cloudwatchlogs.emf.serializers.InstantDeserializer;
import software.amazon.cloudwatchlogs.emf.serializers.InstantSerializer;

/** Represents the MetaData part of the EMF schema. */
@AllArgsConstructor
class Metadata {

    @Getter
    @Setter
    @JsonProperty("Timestamp")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant timestamp;

    @Getter
    @Setter
    @With
    @JsonProperty("CloudWatchMetrics")
    private List<MetricDirective> cloudWatchMetrics;

    private Map<String, Object> customFields;

    Metadata() {
        cloudWatchMetrics = new ArrayList<>();
        timestamp = Instant.now();
        customFields = new ConcurrentHashMap<>();
    }

    /**
     * Create a new MetricDirective and add it to the list of MetricDirectives.
     *
     * @return
     */
    MetricDirective createMetricDirective() {
        MetricDirective newMetricDirective = new MetricDirective();
        cloudWatchMetrics.add(newMetricDirective);
        return newMetricDirective;
    }

    void setMetricDirective(MetricDirective metricDirective) {
        cloudWatchMetrics = new ArrayList<>();
        cloudWatchMetrics.add(metricDirective);
    }

    /**
     * Test if there's any metric added.
     *
     * @return true if no metrics have been added, otherwise, false
     */
    boolean isEmpty() {
        return cloudWatchMetrics.isEmpty()
                || this.cloudWatchMetrics.stream().allMatch(MetricDirective::hasNoMetrics);
    }

    void putCustomMetadata(String key, Object value) {
        customFields.put(key, value);
    }

    @JsonAnyGetter
    Map<String, Object> getCustomMetadata() {
        return this.customFields;
    }
}
