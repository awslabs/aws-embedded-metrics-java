package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/**
 * Represents the MetricDefinition of the EMF schema.
 */
class MetricDefinition {
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
}
