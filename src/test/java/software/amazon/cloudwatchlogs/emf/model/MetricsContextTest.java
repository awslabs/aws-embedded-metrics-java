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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

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

    @SuppressWarnings("unchecked")
    private ArrayList<MetricDefinition> parseMetrics(String event) throws JsonProcessingException {
        JsonMapper objectMapper = new JsonMapper();
        Map<String, Object> metadata_map =
                objectMapper.readValue(event, new TypeReference<Map<String, Object>>() {});
        Map<String, Object> metadata = (Map<String, Object>) metadata_map.get("_aws");
        ArrayList<Map<String, Object>> metricDirectives =
                (ArrayList<Map<String, Object>>) metadata.get("CloudWatchMetrics");
        ArrayList<Map<String, String>> metrics =
                (ArrayList<Map<String, String>>) metricDirectives.get(0).get("Metrics");

        ArrayList<MetricDefinition> metricDefinitions = new ArrayList<>();
        for (Map<String, String> metric : metrics) {
            String name = metric.get("Name");
            Unit unit = Unit.fromValue(metric.get("Unit"));
            double value = (double) metadata_map.get(name);
            metricDefinitions.add(new MetricDefinition(name, unit, value));
        }
        return metricDefinitions;
    }
}
