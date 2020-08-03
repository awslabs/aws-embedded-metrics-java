package software.amazon.awssdk.services.cloudwatchlogs.emf.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import software.amazon.awssdk.services.cloudwatchlogs.emf.exception.EMFClientException;

public class Jackson {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ObjectWriter writer = objectMapper.writer();

    public static String toJsonString(Object value) {
        try {
            return writer.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Returns the deserialized object from the given json string and target class; or null if the
     * given json string is null.
     */
    public static <T> T fromJsonString(String json, Class<T> clazz) {
        return fromJsonString(json, objectMapper, clazz);
    }

    public static <T> T fromJsonString(String json, ObjectMapper objectMapper, Class<T> clazz) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new EMFClientException("Unable to parse Json String.", e);
        }
    }

    public static JsonNode jsonNodeOf(String json) {
        return fromJsonString(json, JsonNode.class);
    }
}
