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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;

class MetricDirectiveTest {
    private final ObjectMapper objectMapper =
            new ObjectMapper().configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);

    @Test
    void testDefaultNamespace() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[]],\"Metrics\":[],\"Namespace\":\"aws-embedded-metrics\"}",
                serializedMetricDirective);
    }

    @Test
    void testSetNamespace() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.setNamespace("test-lambda-metrics");

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[]],\"Metrics\":[],\"Namespace\":\"test-lambda-metrics\"}",
                serializedMetricDirective);
    }

    @Test
    void testPutMetric() throws JsonProcessingException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putMetric("Time", 10);

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[]],\"Metrics\":[{\"Name\":\"Time\",\"Unit\":\"None\"}],\"Namespace\":\"aws-embedded-metrics\"}",
                serializedMetricDirective);
    }

    @Test
    void testPutSameMetricMultipleTimes() {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putMetric("Time", 10);
        metricDirective.putMetric("Time", 20);

        Assertions.assertEquals(1, metricDirective.getAllMetrics().size());
        MetricDefinition[] mds = metricDirective.getAllMetrics().toArray(new MetricDefinition[0]);
        Assertions.assertEquals(Arrays.asList(10d, 20d), mds[0].getValues());
    }

    @Test
    void testPutMetricWithoutUnit() {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putMetric("Time", 10);
        Assertions.assertEquals(Unit.NONE, metricDirective.getMetrics().get("Time").getUnit());
    }

    @Test
    void testPutMetricWithUnit() {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putMetric("Time", 10, Unit.MILLISECONDS);
        Assertions.assertEquals(
                Unit.MILLISECONDS, metricDirective.getMetrics().get("Time").getUnit());
    }

    @Test
    void testPutDimensions() throws JsonProcessingException, InvalidDimensionException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putDimensionSet(
                DimensionSet.of("Region", "us-east-1", "Instance", "inst-1"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[\"Region\",\"Instance\"]],\"Metrics\":[],\"Namespace\":\"aws-embedded-metrics\"}",
                serializedMetricDirective);
    }

    @Test
    void testPutDimensionSetWhenMultipleDimensionSets()
            throws JsonProcessingException, InvalidDimensionException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[\"Region\"],[\"Instance\"]],\"Metrics\":[],\"Namespace\":\"aws-embedded-metrics\"}",
                serializedMetricDirective);
    }

    @Test
    void testPutDimensionSetWhenDuplicateDimensionSets()
            throws JsonProcessingException, InvalidDimensionException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putDimensionSet(new DimensionSet());
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(
                DimensionSet.of("Region", "us-east-1", "Instance", "inst-1"));
        metricDirective.putDimensionSet(
                DimensionSet.of("Instance", "inst-1", "Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));
        metricDirective.putDimensionSet(new DimensionSet());
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(
                DimensionSet.of("Region", "us-east-1", "Instance", "inst-1"));
        metricDirective.putDimensionSet(
                DimensionSet.of("Instance", "inst-1", "Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[],[\"Region\"],[\"Instance\",\"Region\"],[\"Instance\"]],\"Metrics\":[],\"Namespace\":\"aws-embedded-metrics\"}",
                serializedMetricDirective);
    }

    @Test
    void testPutDimensionSetWhenDuplicateDimensionSetsWillSortCorrectly()
            throws JsonProcessingException, InvalidDimensionException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.putDimensionSet(new DimensionSet());
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(
                DimensionSet.of("Region", "us-east-1", "Instance", "inst-1"));
        metricDirective.putDimensionSet(
                DimensionSet.of("Instance", "inst-1", "Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));
        metricDirective.putDimensionSet(
                DimensionSet.of("Region", "us-east-1", "Instance", "inst-1"));
        metricDirective.putDimensionSet(
                DimensionSet.of("Instance", "inst-1", "Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(new DimensionSet());

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[\"Instance\",\"Region\"],[\"Instance\"],[\"Region\"],[]],\"Metrics\":[],\"Namespace\":\"aws-embedded-metrics\"}",
                serializedMetricDirective);
    }

    @Test
    void testGetDimensionAfterSetDimensions() throws InvalidDimensionException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.setDefaultDimensions(DimensionSet.of("Dim", "Default"));
        metricDirective.setDimensions(Arrays.asList(DimensionSet.of("Name", "Test")));

        Assertions.assertEquals(1, metricDirective.getAllDimensions().size());
    }

    @Test
    void testPutDimensionsWhenDefaultDimensionsDefined()
            throws JsonProcessingException, InvalidDimensionException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.setDefaultDimensions(DimensionSet.of("Version", "1"));
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[\"Version\",\"Region\"],[\"Version\",\"Instance\"]],\"Metrics\":[],\"Namespace\":\"aws-embedded-metrics\"}",
                serializedMetricDirective);
    }

    @Test
    void testPutDimensionsAfterSetDimensions()
            throws JsonProcessingException, InvalidDimensionException {
        MetricDirective metricDirective = new MetricDirective();
        metricDirective.setDimensions(Collections.singletonList(DimensionSet.of("Version", "1")));
        metricDirective.putDimensionSet(DimensionSet.of("Region", "us-east-1"));
        metricDirective.putDimensionSet(DimensionSet.of("Instance", "inst-1"));

        String serializedMetricDirective = objectMapper.writeValueAsString(metricDirective);

        Assertions.assertEquals(
                "{\"Dimensions\":[[\"Version\"],[\"Region\"],[\"Instance\"]],\"Metrics\":[],\"Namespace\":\"aws-embedded-metrics\"}",
                serializedMetricDirective);
    }
}
