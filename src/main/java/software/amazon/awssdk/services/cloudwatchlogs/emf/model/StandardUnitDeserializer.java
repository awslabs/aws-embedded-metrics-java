package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import java.io.IOException;

/**
 * JSON deserializer for StandardUnit type.
 */
public class StandardUnitDeserializer extends StdDeserializer<StandardUnit> {
    StandardUnitDeserializer() {
        this(null);
    }

    StandardUnitDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public StandardUnit deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        String value = jp.getValueAsString();
        StandardUnit unit = StandardUnit.fromValue(value);
        return unit;
    }
}
