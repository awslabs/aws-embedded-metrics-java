package software.amazon.awssdk.services.cloudwatchlogs.emf.model;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class EMFLogItemTest {

    @AllArgsConstructor
    class ComplexProperty
    {
        @Getter
        @Setter
        private String stringVal;

        @Getter
        @Setter
        private int intVal;
    }
    @Test
    public void testSerialize() throws JsonProcessingException {
        final String rawLogMessage = "Raw Log Message";
        EMFLogItem logItem = EMFTestUtilities.createComplexLogItem(0);
        logItem.setRawLogMessage(rawLogMessage);

        assertEquals(rawLogMessage, logItem.getRawLogMessage());

        // serialize just the JSON portion
        String metricsJson = logItem.serializeMetrics();
        // serialize the JSON and non-JSON portion
        String fullLog = logItem.serialize();

        // Validate that the JSON doesn't contain the raw log message
        assertFalse(metricsJson.contains(rawLogMessage));
        // Validate the full log does contain the raw log message
        assertTrue(fullLog.contains(rawLogMessage));

        // Validate the only difference between the 2 is the inclusion of the raw log message
        String tmpMetricsJson = String.format("%s%n", metricsJson);
        String difference = StringUtils.difference(tmpMetricsJson, fullLog);
        assertEquals(rawLogMessage, difference);

        // Validate we can deserialize the JSON back into an object
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        RootNode deserializedRootNode = objectMapper.readValue(metricsJson, RootNode.class);
        assertNotNull(deserializedRootNode);
    }


    // test for coverage
    @Test
    public void testGetProperties() {
        EMFLogItem logItem = EMFTestUtilities.createTinyLogItem(0);
        logItem.getProperties();
    }
}
