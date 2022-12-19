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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import software.amazon.cloudwatchlogs.emf.serializers.StorageResolutionFilter;
import software.amazon.cloudwatchlogs.emf.serializers.StorageResolutionSerializer;
import software.amazon.cloudwatchlogs.emf.serializers.UnitDeserializer;
import software.amazon.cloudwatchlogs.emf.serializers.UnitSerializer;

/** Represents the MetricDefinition of the EMF schema. */
@AllArgsConstructor
class MetricDefinition {
    @NonNull
    @Getter
    @JsonProperty("Name")
    private String name;

    @Getter
    @JsonProperty("Unit")
    @JsonSerialize(using = UnitSerializer.class)
    @JsonDeserialize(using = UnitDeserializer.class)
    private Unit unit;

    @Getter
    @Setter
    @JsonProperty("StorageResolution")
    @JsonInclude(
            value = JsonInclude.Include.CUSTOM,
            valueFilter =
                    StorageResolutionFilter.class) // Do not serialize when valueFilter is true
    @JsonSerialize(using = StorageResolutionSerializer.class)
    public StorageResolution storageResolution;

    @JsonIgnore @NonNull @Getter private List<Double> values;

    MetricDefinition(String name) {
        this(name, Unit.NONE, StorageResolution.STANDARD, new ArrayList<>());
    }

    MetricDefinition(String name, double value) {
        this(name, Unit.NONE, StorageResolution.STANDARD, value);
    }

    MetricDefinition(String name, Unit unit, double value) {
        this(name, unit, StorageResolution.STANDARD, new ArrayList<>(Arrays.asList(value)));
    }

    MetricDefinition(String name, StorageResolution storageResolution, double value) {
        this(name, Unit.NONE, storageResolution, new ArrayList<>(Arrays.asList(value)));
    }

    MetricDefinition(String name, Unit unit, StorageResolution storageResolution, double value) {
        this(name, unit, storageResolution, new ArrayList<>(Arrays.asList(value)));
    }

    void addValue(double value) {
        values.add(value);
    }
}
