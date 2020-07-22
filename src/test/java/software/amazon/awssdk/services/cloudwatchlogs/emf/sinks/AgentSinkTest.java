package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

import java.util.Map;

import static junit.framework.TestCase.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
    }

    @Before
    public void setUp() {
        factory = mock(SocketClientFactory.class);

        client = new TestClient();
        when(factory.getClient(any())).thenReturn(client);
    }

    @Test
    public void testAccept() throws JsonProcessingException {
        String logGroupName = "TestLogGroup";
        String logStreamName = "TestLogStream";

        MetricsContext mc = new MetricsContext();

        AgentSink sink  = new AgentSink(
                logGroupName,
                logStreamName,
                Endpoint.DEFAULT_TCP_ENDPOINT,
                factory
        );

        sink.accept(mc);

        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> emf_map = objectMapper.readValue(client.getMessage(), new TypeReference<Map<String, Object>>(){});


        assertEquals(emf_map.get("LogGroupName"), logGroupName);
        assertEquals(emf_map.get("LogStreamName"), logStreamName);
    }

    @Test
    public void testEmptyLogGroupName() throws JsonProcessingException {
        String logGroupName = "";
        AgentSink sink  = new AgentSink(
                logGroupName,
                null,
                Endpoint.DEFAULT_TCP_ENDPOINT,
                factory
        );

        sink.accept(new MetricsContext());
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> emf_map = objectMapper.readValue(client.getMessage(), new TypeReference<Map<String, Object>>(){});

        assertFalse(emf_map.containsKey("LogGroupName"));
        assertFalse(emf_map.containsKey("LogStreamName"));

    }
}
