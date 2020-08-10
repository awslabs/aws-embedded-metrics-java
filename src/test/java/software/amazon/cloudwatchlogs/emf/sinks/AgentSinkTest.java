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

package software.amazon.cloudwatchlogs.emf.sinks;

import static junit.framework.TestCase.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

@SuppressWarnings("unchecked")
public class AgentSinkTest {

    private SocketClientFactory factory;
    private TestClient client;

    class TestClient implements SocketClient {

        private String message;

        @Override
        public void sendMessage(String message) {
            this.message = message;
        }

        public String getMessage() {
            return this.message;
        }

        @Override
        public void close() {}
    }

    @Before
    public void setUp() {
        factory = mock(SocketClientFactory.class);

        client = new TestClient();
        when(factory.getClient(any())).thenReturn(client);
    }

    @Test
    public void testAccept() throws JsonProcessingException {
        String prop = "TestProp";
        String propValue = "TestPropValue";
        String logGroupName = "TestLogGroup";
        String logStreamName = "TestLogStream";

        MetricsContext mc = new MetricsContext();

        mc.putProperty(prop, propValue);
        mc.putMetric("Time", 10);

        AgentSink sink =
                new AgentSink(logGroupName, logStreamName, Endpoint.DEFAULT_TCP_ENDPOINT, factory);

        sink.accept(mc);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> emf_map =
                objectMapper.readValue(
                        client.getMessage(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> metadata = (Map<String, Object>) emf_map.get("_aws");

        assertEquals(emf_map.get(prop), propValue);
        assertEquals(emf_map.get("Time"), 10.0);
        assertEquals(metadata.get("LogGroupName"), logGroupName);
        assertEquals(metadata.get("LogStreamName"), logStreamName);
    }

    @Test
    public void testEmptyLogGroupName() throws JsonProcessingException {
        String logGroupName = "";
        AgentSink sink = new AgentSink(logGroupName, null, Endpoint.DEFAULT_TCP_ENDPOINT, factory);
        MetricsContext mc = new MetricsContext();
        mc.putMetric("Time", 10);

        sink.accept(mc);
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> emf_map =
                objectMapper.readValue(
                        client.getMessage(), new TypeReference<Map<String, Object>>() {});
        Map<String, Object> metadata = (Map<String, Object>) emf_map.get("_aws");

        assertFalse(metadata.containsKey("LogGroupName"));
        assertFalse(metadata.containsKey("LogStreamName"));
    }
}
