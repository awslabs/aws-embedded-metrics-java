package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

import static org.junit.Assert.assertEquals;

public class MetricDefinitionTest {

    @Test(expected = NullPointerException.class)
    public void testThrowExceptionIfNameIsNull() {
        new MetricDefinition(null);
    }

    @Test
    public void testSerializeMetricDefinitionWithoutUnit() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        MetricDefinition metricDefinition = new MetricDefinition("Time");
        String metricString = objectMapper.writeValueAsString(metricDefinition);

        assertEquals(metricString, "{\"Name\":\"Time\",\"Unit\":\"None\"}");
    }

    @Test
    public void testSerializeMetricDefinition() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        MetricDefinition metricDefinition = new MetricDefinition("Time", StandardUnit.MILLISECONDS);
        String metricString = objectMapper.writeValueAsString(metricDefinition);

        assertEquals(metricString, "{\"Name\":\"Time\",\"Unit\":\"Milliseconds\"}");
    }
}
