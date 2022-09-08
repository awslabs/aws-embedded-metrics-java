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
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;

public class RootNodeTest {

    @Test
    public void testPutProperty() throws DimensionSetExceededException {
        RootNode rootNode = new RootNode();
        rootNode.putProperty("Property", "Value");

        assertEquals(rootNode.getTargetMembers().get("Property"), "Value");
    }

    @Test
    public void testPutSamePropertyMultipleTimes() throws DimensionSetExceededException {
        RootNode rootNode = new RootNode();
        rootNode.putProperty("Property", "Value");
        rootNode.putProperty("Property", "NewValue");

        assertEquals(rootNode.getTargetMembers().get("Property"), "NewValue");
    }

    @Test
    public void testGetDimension() throws DimensionSetExceededException {
        RootNode rootNode = new RootNode();
        MetricDirective metricDirective = rootNode.getAws().createMetricDirective();
        metricDirective.putDimensionSet(DimensionSet.of("Dim1", "DimValue1"));

        assertEquals(rootNode.getTargetMembers().get("Dim1"), "DimValue1");
    }

    @Test
    public void testGetTargetMembers() throws DimensionSetExceededException {
        RootNode rootNode = new RootNode();
        MetricsContext mc = new MetricsContext(rootNode);

        // Put same metric multiple times
        mc.putMetric("Count", 10.0);
        mc.putMetric("Count", 20.0);

        mc.putMetric("Latency", 100.0, Unit.MILLISECONDS);

        mc.putDimension("Dim1", "DimVal1");

        mc.putProperty("Prop1", "PropValue1");

        assertEquals(rootNode.getTargetMembers().get("Count"), Arrays.asList(10.0, 20.0));
        assertEquals(rootNode.getTargetMembers().get("Latency"), 100.0);
        assertEquals(rootNode.getTargetMembers().get("Dim1"), "DimVal1");
        assertEquals(rootNode.getTargetMembers().get("Prop1"), "PropValue1");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSerializeRootNode() throws JsonProcessingException {
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

        assertEquals(emf_map.keySet().size(), 5);
        assertEquals(emf_map.get("Region"), "us-east-1");
        assertEquals(emf_map.get("Property"), "PropertyValue");
        assertEquals(emf_map.get("DefaultDim"), "DefaultDimValue");
        assertEquals(emf_map.get("Count"), 10.0);

        Map<String, Object> metadata = (Map<String, Object>) emf_map.get("_aws");
        assertTrue(metadata.containsKey("Timestamp"));
        assertTrue(metadata.containsKey("CloudWatchMetrics"));
    }

    @Test
    public void testSerializeRootNodeWithoutAnyMetrics() throws JsonProcessingException {
        RootNode root = new RootNode();
        String property = "foo";
        String value = "bar";
        root.putProperty(property, value);

        assertEquals(root.serialize(), "{\"foo\":\"bar\"}");
    }
}
