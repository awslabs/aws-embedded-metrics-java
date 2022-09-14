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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;

class RootNodeTest {

    @Test
    void testPutProperty() {
        RootNode rootNode = new RootNode();
        rootNode.putProperty("Property", "Value");

        Assertions.assertEquals("Value", rootNode.getTargetMembers().get("Property"));
    }

    @Test
    void testPutSamePropertyMultipleTimes() {
        RootNode rootNode = new RootNode();
        rootNode.putProperty("Property", "Value");
        rootNode.putProperty("Property", "NewValue");

        Assertions.assertEquals("NewValue", rootNode.getTargetMembers().get("Property"));
    }

    @Test
    void testGetDimension() throws InvalidDimensionException {
        RootNode rootNode = new RootNode();
        MetricDirective metricDirective = rootNode.getAws().createMetricDirective();
        metricDirective.putDimensionSet(DimensionSet.of("Dim1", "DimValue1"));

        Assertions.assertEquals("DimValue1", rootNode.getTargetMembers().get("Dim1"));
    }

    @Test
    void testGetTargetMembers() throws InvalidMetricException, InvalidDimensionException {
        RootNode rootNode = new RootNode();
        MetricsContext mc = new MetricsContext(rootNode);

        // Put same metric multiple times
        mc.putMetric("Count", 10.0);
        mc.putMetric("Count", 20.0);

        mc.putMetric("Latency", 100.0, Unit.MILLISECONDS);

        mc.putDimension("Dim1", "DimVal1");

        mc.putProperty("Prop1", "PropValue1");

        Assertions.assertEquals(List.of(10.0, 20.0), rootNode.getTargetMembers().get("Count"));
        Assertions.assertEquals(100.0, rootNode.getTargetMembers().get("Latency"));
        Assertions.assertEquals("DimVal1", rootNode.getTargetMembers().get("Dim1"));
        Assertions.assertEquals("PropValue1", rootNode.getTargetMembers().get("Prop1"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testSerializeRootNode()
            throws JsonProcessingException, InvalidMetricException, InvalidDimensionException {
        MetricsContext mc = new MetricsContext();

        mc.setDefaultDimensions(DimensionSet.of("DefaultDim", "DefaultDimValue"));
        mc.putDimension(DimensionSet.of("Region", "us-east-1"));
        mc.putMetric("Count", 10);
        mc.putProperty("Property", "PropertyValue");

        ObjectMapper objectMapper = new ObjectMapper();
        List<String> emf_logs = mc.serialize();
        Map<String, Object> emf_map =
                objectMapper.readValue(
                        emf_logs.get(0), new TypeReference<Map<String, Object>>() {});

        Assertions.assertEquals(5, emf_map.keySet().size());
        Assertions.assertEquals("us-east-1", emf_map.get("Region"));
        Assertions.assertEquals("PropertyValue", emf_map.get("Property"));
        Assertions.assertEquals("DefaultDimValue", emf_map.get("DefaultDim"));
        Assertions.assertEquals(10.0, emf_map.get("Count"));

        Map<String, Object> metadata = (Map<String, Object>) emf_map.get("_aws");
        Assertions.assertTrue(metadata.containsKey("Timestamp"));
        Assertions.assertTrue(metadata.containsKey("CloudWatchMetrics"));
    }

    @Test
    void testSerializeRootNodeWithoutAnyMetrics() throws JsonProcessingException {
        RootNode root = new RootNode();
        String property = "foo";
        String value = "bar";
        root.putProperty(property, value);

        Assertions.assertEquals("{\"foo\":\"bar\"}", root.serialize());
    }
}
