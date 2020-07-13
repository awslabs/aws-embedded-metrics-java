package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Serialize an AggregationCollection as an array or Aggregations.
 */
class AggregationCollectionSerializer extends StdSerializer<AggregationCollection> {
    AggregationCollectionSerializer() {
        this(null);
    }

    AggregationCollectionSerializer(Class<AggregationCollection> t) {
        super(t);
    }

    @Override
    public void serialize(AggregationCollection value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        Set<Aggregation> uniqueAggregations = new HashSet<>();
        uniqueAggregations.addAll(value.getAggregations());

        jgen.writeObject(uniqueAggregations);
    }
}
