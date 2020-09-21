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
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.With;

/** Represents the root of the EMF schema. */
@AllArgsConstructor
@JsonFilter("emptyMetricFilter")
class RootNode {
    @Getter
    @With
    @JsonProperty("_aws")
    private Metadata aws;

    private Map<String, Object> properties;
    private ObjectMapper objectMapper;

    RootNode() {
        final SimpleFilterProvider filterProvider =
                new SimpleFilterProvider().addFilter("emptyMetricFilter", new EmptyMetricsFilter());
        aws = new Metadata();
        properties = new HashMap<>();
        objectMapper = new ObjectMapper();
        objectMapper.setFilterProvider(filterProvider);
    }

    public void putProperty(String key, Object value) {
        properties.put(key, value);
    }

    Map<String, Object> getProperties() {
        return properties;
    }

    /** Return the target members that are referenced by metrics, dimensions and properties. */
    @JsonAnyGetter
    Map<String, Object> getTargetMembers() {
        Map<String, Object> targetMembers = new HashMap<>();
        targetMembers.putAll(properties);
        targetMembers.putAll(getDimensions());
        for (MetricDirective metricDirective : aws.getCloudWatchMetrics()) {
            for (MetricDefinition metric : metricDirective.getMetrics().values()) {
                List<Double> values = metric.getValues();
                targetMembers.put(metric.getName(), values.size() == 1 ? values.get(0) : values);
            }
        }
        return targetMembers;
    }

    /** Return a list of all dimensions that are referenced by each dimension set. */
    Map<String, String> getDimensions() {
        Map<String, String> dimensions = new HashMap<>();
        for (MetricDirective mc : aws.getCloudWatchMetrics()) {
            for (DimensionSet dimensionSet : mc.getAllDimensions()) {
                dimensions.putAll(dimensionSet.getDimensionRecords());
            }
        }
        return dimensions;
    }

    Map<String, MetricDefinition> metrics() {
        return aws.getCloudWatchMetrics().get(0).getMetrics();
    }

    String serialize() throws JsonProcessingException {
        return objectMapper.writeValueAsString(this);
    }
}
