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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.Constants;

public class MetricsContextTest {

    @Test
    public void testSerializeLessThan100Metrics() throws JsonProcessingException {
        MetricsContext mc = new MetricsContext();
        int metricCount = 10;
        for (int i = 0; i < metricCount; i++) {
            String key = "Metric-" + i;
            mc.putMetric(key, i);
        }

        List<String> events = mc.serialize();
        assertEquals(1, events.size());

        List<MetricDefinition> metrics = parseMetrics(events.get(0));
        assertEquals(metrics.size(), metricCount);
        for (MetricDefinition metric : metrics) {
            MetricDefinition originalMetric = mc.getRootNode().metrics().get(metric.getName());
            assertEquals(originalMetric.getName(), metric.getName());
            assertEquals(originalMetric.getUnit(), metric.getUnit());
        }
    }

    @Test
    public void testSerializeMoreThen100Metrics() throws JsonProcessingException {
        MetricsContext mc = new MetricsContext();
        int metricCount = 253;
        int expectedEventCount = 3;
        for (int i = 0; i < metricCount; i++) {
            String key = "Metric-" + i;
            mc.putMetric(key, i);
        }

        List<String> events = mc.serialize();
        assertEquals(expectedEventCount, events.size());

        List<MetricDefinition> allMetrics = new ArrayList<>();
        for (String event : events) {
            allMetrics.addAll(parseMetrics(event));
        }
        assertEquals(metricCount, allMetrics.size());
        for (MetricDefinition metric : allMetrics) {
            MetricDefinition originalMetric = mc.getRootNode().metrics().get(metric.getName());
            assertEquals(originalMetric.getName(), metric.getName());
            assertEquals(originalMetric.getUnit(), metric.getUnit());
        }
    }

    @Test
    public void testSerializeAMetricWith101DataPoints() throws JsonProcessingException {
        MetricsContext mc = new MetricsContext();
        int dataPointCount = 101;
        int expectedEventCount = 2;
        String metricName = "metric";
        for (int i = 0; i < dataPointCount; i++) {
            mc.putMetric(metricName, i);
        }

        List<String> events = mc.serialize();
        assertEquals(expectedEventCount, events.size());
        List<MetricDefinition> allMetrics = new ArrayList<>();
        for (String event : events) {
            allMetrics.addAll(parseMetrics(event));
        }
        List<Double> expectedValues = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_DATAPOINTS_PER_METRIC; i++) {
            expectedValues.add((double) i);
        }
        assertEquals(expectedValues, allMetrics.get(0).getValues());
        assertTrue(allMetrics.get(1).getValues().equals(Arrays.asList(100.0)));
    }

    @Test
    public void testSerializeMetricsWith101DataPoints() throws JsonProcessingException {
        MetricsContext mc = new MetricsContext();
        int dataPointCount = 101;
        int expectedEventCount = 2;
        String metricName = "metric1";
        for (int i = 0; i < dataPointCount; i++) {
            mc.putMetric(metricName, i);
        }
        mc.putMetric("metric2", 2);

        List<String> events = mc.serialize();
        assertEquals(expectedEventCount, events.size());

        List<MetricDefinition> metricsFromEvent1 = parseMetrics(events.get(0));
        List<MetricDefinition> metricsFromEvent2 = parseMetrics(events.get(1));

        assertEquals(2, metricsFromEvent1.size());
        List<Double> expectedValues = new ArrayList<>();
        for (int i = 0; i < Constants.MAX_DATAPOINTS_PER_METRIC; i++) {
            expectedValues.add((double) i);
        }
        assertEquals(expectedValues, metricsFromEvent1.get(0).getValues());
        assertEquals(Arrays.asList(2.0), metricsFromEvent1.get(1).getValues());

        assertEquals(1, metricsFromEvent2.size());
        assertEquals(Arrays.asList(100.0), metricsFromEvent2.get(0).getValues());
    }

    @Test
    public void testSerializeZeroMetric() throws JsonProcessingException {
        MetricsContext mc = new MetricsContext();
        mc.putDimension(DimensionSet.of("Region", "IAD"));
        List<String> events = mc.serialize();

        int expectedEventCount = 1;
        assertEquals(expectedEventCount, events.size());

        Map<String, Object> rootNode = parseRootNode(events.get(0));
        // If there's no metric added, the _aws would be filtered out from the log event
        assertFalse(rootNode.containsKey("_aws"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetTimestamp() throws JsonProcessingException {
        MetricsContext mc = new MetricsContext();
        mc.putMetric("Metric", 0);

        Instant now = Instant.now();
        mc.setTimestamp(now);

        List<String> events = mc.serialize();

        int expectedEventCount = 1;
        assertEquals(expectedEventCount, events.size());
        Map<String, Object> rootNode = parseRootNode(events.get(0));

        assertTrue(rootNode.containsKey("_aws"));
        Map<String, Object> metadata = (Map<String, Object>) rootNode.get("_aws");

        assertTrue(metadata.containsKey("Timestamp"));
        assertEquals(metadata.get("Timestamp"), now.toEpochMilli());
    }

    @SuppressWarnings("unchecked")
    private ArrayList<MetricDefinition> parseMetrics(String event) throws JsonProcessingException {
        Map<String, Object> rootNode = parseRootNode(event);
        Map<String, Object> metadata = (Map<String, Object>) rootNode.get("_aws");
        ArrayList<Map<String, Object>> metricDirectives =
                (ArrayList<Map<String, Object>>) metadata.get("CloudWatchMetrics");
        ArrayList<Map<String, String>> metrics =
                (ArrayList<Map<String, String>>) metricDirectives.get(0).get("Metrics");

        ArrayList<MetricDefinition> metricDefinitions = new ArrayList<>();
        for (Map<String, String> metric : metrics) {
            String name = metric.get("Name");
            Unit unit = Unit.fromValue(metric.get("Unit"));
            Object value = rootNode.get(name);
            if (value instanceof ArrayList) {
                metricDefinitions.add(new MetricDefinition(name, unit, (ArrayList) value));
            } else {
                metricDefinitions.add(new MetricDefinition(name, unit, (double) value));
            }
        }
        return metricDefinitions;
    }

    private Map<String, Object> parseRootNode(String event) throws JsonProcessingException {
        return new JsonMapper().readValue(event, new TypeReference<>() {});
    }
}
