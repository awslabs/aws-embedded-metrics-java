/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package software.amazon.cloudwatchlogs.emf.model;

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
