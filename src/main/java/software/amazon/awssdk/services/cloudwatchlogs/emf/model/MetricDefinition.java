package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import lombok.AllArgsConstructor;
import software.amazon.awssdk.services.cloudwatchlogs.emf.serializers.StandardUnitDeserializer;
import software.amazon.awssdk.services.cloudwatchlogs.emf.serializers.StandardUnitSerializer;

/**
 * Represents the MetricDefinition of the EMF schema.
 */


@AllArgsConstructor
class MetricDefinition {
    @NonNull
    @Setter @Getter
    @JsonProperty("Name")
    private String name;

    @Setter @Getter
    @JsonProperty("Unit")
    @JsonSerialize(using = StandardUnitSerializer.class)
    @JsonDeserialize(using = StandardUnitDeserializer.class)
    private StandardUnit unit;

    MetricDefinition(String name) {
        this(name, StandardUnit.NONE);
    }
}
