package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import static org.junit.Assert.assertEquals;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class MetricDirectiveTest {
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testDefaultNamespace() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        assertEquals(
                serializedMetricDirective,
                "{\"Namespace\":\"aws-embedded-metrics\",\"Metrics\":[],\"Dimensions\":[[]]}");
    }

    @Test
    public void testSetNamespace() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.setNamespace("test-lambda-metrics");

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        assertEquals(
                serializedMetricDirective,
                "{\"Namespace\":\"test-lambda-metrics\",\"Metrics\":[],\"Dimensions\":[[]]}");
    }

    @Test
    public void testPutMetric() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putMetric(new MetricDefinition("Time"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        assertEquals(
                serializedMetricDirective,
                "{\"Namespace\":\"aws-embedded-metrics\",\"Metrics\":[{\"Name\":\"Time\",\"Unit\":\"None\"}],\"Dimensions\":[[]]}");
    }

    @Test
    public void testPutDimensions() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putDimensionSet(
                DimensionSet.of("Region", "us-east-1", "Instance", "inst-1"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        assertEquals(
                serializedMetricDirective,
                "{\"Namespace\":\"aws-embedded-metrics\",\"Metrics\":[],\"Dimensions\":[[\"Region\",\"Instance\"]]}");
    }

    @Test
    public void testPutMultipleDimensionSets() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        assertEquals(
                serializedMetricDirective,
                "{\"Namespace\":\"aws-embedded-metrics\",\"Metrics\":[],\"Dimensions\":[[\"Region\"],[\"Instance\"]]}");
    }

    @Test
    public void testPutDimensionsWhenDefaultDimensionsDefined() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.setDefaultDimensions(DimensionSet.of("Version", "1"));
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        assertEquals(
                serializedMetricDirective,
                "{\"Namespace\":\"aws-embedded-metrics\",\"Metrics\":[],\"Dimensions\":[[\"Version\",\"Region\"],[\"Version\",\"Instance\"]]}");
    }
}
