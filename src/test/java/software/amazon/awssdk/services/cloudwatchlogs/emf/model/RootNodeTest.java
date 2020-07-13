package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RootNodeTest {

    @Test
    public void testPutMetric() {
        RootNode rootNode = new RootNode();
        rootNode.putMetric("Time", 10.0);

        assertEquals(rootNode.getTargetMembers().get("Time"), 10.0);
    }

    @Test
    public void testPutSameMetricMultipleTimes() {
        RootNode rootNode = new RootNode();
        rootNode.putMetric("Time", 10.0);
        rootNode.putMetric("Time", 20.0);

        assertEquals(rootNode.getTargetMembers().get("Time"), Arrays.asList(10.0, 20.0));
    }

    @Test
    public void testPutProperty() {
        RootNode rootNode = new RootNode();
        rootNode.putProperty("Property", "Value");

        assertEquals(rootNode.getTargetMembers().get("Property"), "Value");
    }

    @Test
    public void testGetDimension() {
        RootNode rootNode = new RootNode();
        MetricDirective metricDirective = rootNode.getAws().createMetricDirective();
        metricDirective.putDimensionSet(DimensionSet.of("Dim1", "DimValue1"));

        assertEquals(rootNode.getTargetMembers().get("Dim1"), "DimValue1");
    }

    @Test
    public void testSerializeRootNode() throws JsonProcessingException {
        MetricsContext mc = new MetricsContext();

        mc.setDefaultDimensions(DimensionSet.of("DefaultDim", "DefaultDimValue"));
        mc.putDimension(DimensionSet.of("Region", "us-east-1"));
        mc.putMetric("Count", 10);
        mc.putProperty("Property", "PropertyValue");

        ObjectMapper objectMapper = new ObjectMapper();
        String emf_log = objectMapper.writeValueAsString(mc.getRootNode());
        Map<String, Object> emf_map = objectMapper.readValue(emf_log, new TypeReference<Map<String, Object>>(){});

        assertEquals(emf_map.keySet().size(), 5);
        assertEquals(emf_map.get("Region"), "us-east-1");
        assertEquals(emf_map.get("Property"), "PropertyValue");
        assertEquals(emf_map.get("DefaultDim"), "DefaultDimValue");
        assertEquals(emf_map.get("Count"), 10.0);

        Map<String, Object> metadata = (Map<String, Object>) emf_map.get("_aws");
        assertTrue(metadata.containsKey("Timestamp"));
        assertTrue(metadata.containsKey("CloudWatchMetrics"));
    }
}
