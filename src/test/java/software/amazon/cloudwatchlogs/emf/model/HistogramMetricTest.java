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
import static org.junit.Assert.assertThrows;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

public class HistogramMetricTest {
    @Test
    public void testSerializeHistogramMetricWithoutUnitWithHighStorageResolution()
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        HistogramMetric histogramMetric =
                HistogramMetric.builder()
                        .storageResolution(StorageResolution.HIGH)
                        .addValue(10)
                        .name("Time")
                        .build();
        String metricString = objectMapper.writeValueAsString(histogramMetric);

        assertEquals("{\"Name\":\"Time\",\"Unit\":\"None\",\"StorageResolution\":1}", metricString);
    }

    @Test
    public void testSerializeHistogramMetricWithUnitWithoutStorageResolution()
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        HistogramMetric histogramMetric =
                HistogramMetric.builder().unit(Unit.MILLISECONDS).name("Time").addValue(10).build();
        String metricString = objectMapper.writeValueAsString(histogramMetric);

        assertEquals("{\"Name\":\"Time\",\"Unit\":\"Milliseconds\"}", metricString);
    }

    @Test
    public void testSerializeHistogramMetricWithoutUnitWithStandardStorageResolution()
            throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        HistogramMetric histogramMetric =
                HistogramMetric.builder()
                        .storageResolution(StorageResolution.STANDARD)
                        .name("Time")
                        .addValue(10)
                        .build();
        String metricString = objectMapper.writeValueAsString(histogramMetric);

        assertEquals("{\"Name\":\"Time\",\"Unit\":\"None\"}", metricString);
    }

    @Test
    public void testSerializeHistogramMetricWithoutUnit() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        HistogramMetric histogramMetric = HistogramMetric.builder().name("Time").build();
        String metricString = objectMapper.writeValueAsString(histogramMetric);

        assertEquals("{\"Name\":\"Time\",\"Unit\":\"None\"}", metricString);
    }

    @Test
    public void testSerializeHistogramMetric() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        HistogramMetric histogramMetric =
                HistogramMetric.builder()
                        .unit(Unit.MILLISECONDS)
                        .storageResolution(StorageResolution.HIGH)
                        .name("Time")
                        .addValue(10)
                        .build();
        String metricString = objectMapper.writeValueAsString(histogramMetric);

        assertEquals(
                "{\"Name\":\"Time\",\"Unit\":\"Milliseconds\",\"StorageResolution\":1}",
                metricString);
    }

    @Test
    public void testAddValues() {
        HistogramMetric.HistogramMetricBuilder builder = HistogramMetric.builder();
        builder.addValue(10);

        assertEquals(1, builder.getValues().count);
        assertEquals(10d, builder.getValues().max, 1e-5);
        assertEquals(10d, builder.getValues().min, 1e-5);
        assertEquals(10d, builder.getValues().sum, 1e-5);
        assertEquals(1, builder.getValues().values.size());
        assertEquals(1, builder.getValues().counts.size());

        builder.addValue(200);
        assertEquals(2, builder.getValues().count);
        assertEquals(200d, builder.getValues().max, 1e-5);
        assertEquals(10d, builder.getValues().min, 1e-5);
        assertEquals(210d, builder.getValues().sum, 1e-5);
        assertEquals(2, builder.getValues().values.size());
        assertEquals(2, builder.getValues().counts.size());
    }

    @Test
    public void testManyAddValues() {
        HistogramMetric.HistogramMetricBuilder histBuilder = HistogramMetric.builder();
        for (int i = 1; i < 100; i++) {
            histBuilder.addValue(i);
        }
        histBuilder.build();
    }

    @Test
    public void testBuildBuilder() {
        HistogramMetric histogramMetric = HistogramMetric.builder().addValue(10).build();
        assertEquals(histogramMetric.getValues(), histogramMetric.getValues());

        assertEquals(histogramMetric.name, null);
        histogramMetric.setName("test");
        assertEquals(histogramMetric.name, "test");
    }

    @Test
    public void testCreateImmutableHistogramMetric() {
        HistogramMetric histogram =
                new HistogramMetric(
                        Unit.NONE,
                        StorageResolution.STANDARD,
                        Arrays.asList(10., 20., 30.),
                        Arrays.asList(1, 2, 3));
        assertEquals(6, histogram.getValues().count);
        assertEquals(30d, histogram.getValues().max, 1e-5);
        assertEquals(10d, histogram.getValues().min, 1e-5);
        assertEquals(140d, histogram.getValues().sum, 1e-5);
        assertEquals(3, histogram.getValues().values.size());
        assertEquals(3, histogram.getValues().counts.size());
    }

    @Test
    public void testImpossibleHistogramMetric() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new HistogramMetric(
                                Unit.NONE,
                                StorageResolution.STANDARD,
                                Arrays.asList(10., 20., 30.),
                                Arrays.asList(10, 20))); // Array Size mismatch
    }
}
