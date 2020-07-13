package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

/**
 * Deserializer for Aggregations to get an Aggregation object from an array to match EMF spec.
 */
class AggregationDeserializer extends StdDeserializer<Aggregation> {
    AggregationDeserializer() {
        this(null);
    }

    AggregationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Aggregation deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        String[] dimensions = jp.readValueAs(String[].class);
        return new Aggregation(dimensions);
    }
}
