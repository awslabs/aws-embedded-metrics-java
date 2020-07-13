package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Deserialize an AggregationCollection as an array or Aggregations.
 * Deserialization is only used for testing
 */
class AggregationCollectionDeserializer extends StdDeserializer<AggregationCollection> {
    AggregationCollectionDeserializer() {
        this(null);
    }

    AggregationCollectionDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public AggregationCollection deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        final Aggregation[] aggregations = jp.readValueAs(Aggregation[].class);
        return new AggregationCollection(aggregations);
    }
}
