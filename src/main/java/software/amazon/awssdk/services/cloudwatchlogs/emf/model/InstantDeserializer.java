package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.time.Instant;

/**
 * Deserialize Instant from a Long epoch millisecond timestamp.
 */
public class InstantDeserializer extends StdDeserializer<Instant> {
    InstantDeserializer() {
        this(null);
    }

    InstantDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Instant deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException {
        long timestamp = jp.readValueAs(Long.class);
        return Instant.ofEpochMilli(timestamp);
    }
}

