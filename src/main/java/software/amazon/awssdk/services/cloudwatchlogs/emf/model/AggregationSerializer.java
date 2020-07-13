package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

/**
 * Serializer for Aggregations to make an Aggregation object just look like an array to match EMF spec.
 */
class AggregationSerializer extends StdSerializer<Aggregation> {
    AggregationSerializer() {
        this(null);
    }

    AggregationSerializer(Class<Aggregation> t) {
        super(t);
    }

    @Override
    public void serialize(Aggregation value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        // Just serialize dimensions as an array.
        jgen.writeObject(value.getDimensions());
    }
}
