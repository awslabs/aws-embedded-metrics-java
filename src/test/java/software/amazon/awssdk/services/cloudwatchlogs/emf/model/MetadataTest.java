package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class MetadataTest {

    @Test
    public void testSerializeMetadata() throws JsonProcessingException {
        Metadata metadata = new Metadata();
        Instant now = Instant.now();
        metadata.setTimestamp(now);
        JsonMapper objectMapper = new JsonMapper();
        String output = objectMapper.writeValueAsString(metadata);


        Map<String, Object> metadata_map = objectMapper.readValue(output, new TypeReference<Map<String, Object>>(){});

        assertEquals(metadata_map.keySet().size(), 2);
        assertEquals(metadata_map.get("Timestamp"), now.toEpochMilli());
        assertEquals(metadata_map.get("CloudWatchMetrics"), new ArrayList());
    }

    @Test
    public void testSerializeMetadataWithCustomValue() throws JsonProcessingException {
        Metadata metadata = new Metadata();
        Instant now = Instant.now();
        metadata.setTimestamp(now);
        String property = "foo";
        String expectedValue = "bar";
        metadata.putCustomMetadata(property, expectedValue);

        JsonMapper objectMapper = new JsonMapper();
        String output = objectMapper.writeValueAsString(metadata);


        Map<String, Object> metadata_map = objectMapper.readValue(output, new TypeReference<Map<String, Object>>(){});

        assertEquals(metadata_map.keySet().size(), 3);
        assertEquals(metadata_map.get("Timestamp"), now.toEpochMilli());
        assertEquals(metadata_map.get("CloudWatchMetrics"), new ArrayList());
        assertEquals(metadata_map.get(property), expectedValue);
    }
}
