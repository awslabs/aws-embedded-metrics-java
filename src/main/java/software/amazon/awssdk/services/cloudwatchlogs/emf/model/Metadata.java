package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.cloudwatchlogs.emf.serializers.InstantDeserializer;
import software.amazon.awssdk.services.cloudwatchlogs.emf.serializers.InstantSerializer;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the MetaData part of the EMF schema.
 */
class Metadata {

    @Getter @Setter
    @JsonProperty("Timestamp")
    @JsonFormat(shape = JsonFormat.Shape.NUMBER, timezone = "UTC")
    @JsonSerialize(using = InstantSerializer.class)
    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant timestamp;

    @Getter @Setter
    @JsonProperty("CloudWatchMetrics")
    private List<MetricDirective> cloudWatchMetrics;

    Metadata() {
        cloudWatchMetrics = new ArrayList<>();
        timestamp = Instant.now();
    }


    /**
     * Create a new MetricDirective and add it to the list of MetricDirectives.
     * @return
     */
    MetricDirective createMetricDirective() {
        MetricDirective newMetricDirective = new MetricDirective();
        cloudWatchMetrics.add(newMetricDirective);
        return newMetricDirective;
    }

    /**
     * Test if there's any metric added.
     * @return true if no metrics have been added, otherwise, false
     */
    boolean isEmpty() {
        return cloudWatchMetrics.isEmpty() || this.cloudWatchMetrics.stream()
                .allMatch(MetricDirective::hasNoMetrics);
    }

}
