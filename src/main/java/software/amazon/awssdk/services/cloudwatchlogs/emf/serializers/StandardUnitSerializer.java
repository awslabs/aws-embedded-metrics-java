package software.amazon.awssdk.services.cloudwatchlogs.emf.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.io.IOException;

/**
 * JSON serializer for StandardUnit type.
 */
public class StandardUnitSerializer extends StdSerializer<StandardUnit> {
    StandardUnitSerializer() {
        this(null);
    }

    StandardUnitSerializer(Class<StandardUnit> t) {
        super(t);
    }

    @Override
    public void serialize(StandardUnit value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {

        String str = value.toString();
        jgen.writeString(str);
    }
}
