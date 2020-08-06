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

package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatchlogs.emf.serializers.StandardUnitDeserializer;
import software.amazon.awssdk.services.cloudwatchlogs.emf.serializers.StandardUnitSerializer;

/** Represents the MetricDefinition of the EMF schema. */
@AllArgsConstructor
class MetricDefinition {
    @NonNull
    @Setter
    @Getter
    @JsonProperty("Name")
    private String name;

    @Setter
    @Getter
    @JsonProperty("Unit")
    @JsonSerialize(using = StandardUnitSerializer.class)
    @JsonDeserialize(using = StandardUnitDeserializer.class)
    private StandardUnit unit;

    MetricDefinition(String name) {
        this(name, StandardUnit.NONE);
    }
}
