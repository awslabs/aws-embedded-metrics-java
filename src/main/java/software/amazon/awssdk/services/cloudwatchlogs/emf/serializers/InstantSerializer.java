package software.amazon.awssdk.services.cloudwatchlogs.emf.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.time.Instant;

/** Serialize Instant from a Long epoch millisecond timestamp. */
public class InstantSerializer extends StdSerializer<Instant> {
    InstantSerializer() {
        this(null);
    }

    InstantSerializer(Class<Instant> t) {
        super(t);
    }

    @Override
    public void serialize(Instant value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        // Just serialize dimensions as an array.
        jgen.writeNumber(value.toEpochMilli());
    }
}
