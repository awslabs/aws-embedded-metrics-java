package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

/**
 * A collection of dimension aggregations
 * This encapsulation allows us to create a custom serializer for the Aggregation Collection so
 * Aggregations can be stored as a list, but uniqued on serialization.
 */
@JsonSerialize(using = AggregationCollectionSerializer.class)
@JsonDeserialize(using = AggregationCollectionDeserializer.class)
class AggregationCollection {
    // Must be a list, since aggregations CAN change, we will unique before serializing.
    @Getter
    private List<Aggregation> aggregations = new ArrayList<>();;

    AggregationCollection(Aggregation...aggregations) {
        for (Aggregation aggregation : aggregations) {
            this.aggregations.add(aggregation);
        }
    }


    void addAggregation(Aggregation aggregation) {
        getAggregations().add(aggregation);
    }
}
