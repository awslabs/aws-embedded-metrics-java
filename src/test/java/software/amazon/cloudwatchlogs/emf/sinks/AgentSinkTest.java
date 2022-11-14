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
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.exception.EMFClientException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidMetricException;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.retry.RetryStrategy;

@SuppressWarnings("unchecked")
public class AgentSinkTest {

    @Test
    public void testAccept() throws JsonProcessingException, InvalidMetricException {
        // arrange
        Fixture fixture = new Fixture();
        String prop = "TestProp";
        String propValue = "TestPropValue";
        String logGroupName = "TestLogGroup";
        String logStreamName = "TestLogStream";

        MetricsContext mc = new MetricsContext();

        mc.putProperty(prop, propValue);
        mc.putMetric("Time", 10);

        AgentSink sink =
                new AgentSink(
                        logGroupName,
                        logStreamName,
                        Endpoint.DEFAULT_TCP_ENDPOINT,
                        fixture.factory,
                        1,
                        InstantRetryStrategy::new);

        // act
        sink.accept(mc);
        sink.shutdown().join();

        // assert
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> emf_map =
                objectMapper.readValue(
                        fixture.client.getMessages().get(0),
                        new TypeReference<Map<String, Object>>() {});
        Map<String, Object> metadata = (Map<String, Object>) emf_map.get("_aws");

        assertEquals(propValue, emf_map.get(prop));
        assertEquals(10.0, emf_map.get("Time"));
        assertEquals(logGroupName, metadata.get("LogGroupName"));
        assertEquals(logStreamName, metadata.get("LogStreamName"));
    }

    @Test
    public void testEmptyLogGroupName() throws JsonProcessingException, InvalidMetricException {
        // arrange
        Fixture fixture = new Fixture();
        String logGroupName = "";
        AgentSink sink =
                new AgentSink(
                        logGroupName,
                        null,
                        Endpoint.DEFAULT_TCP_ENDPOINT,
                        fixture.factory,
                        1,
                        InstantRetryStrategy::new);
        MetricsContext mc = new MetricsContext();
        mc.putMetric("Time", 10);

        // act
        sink.accept(mc);
        sink.shutdown().join();

        // assert
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> emf_map =
                objectMapper.readValue(
                        fixture.client.getMessages().get(0),
                        new TypeReference<Map<String, Object>>() {});
        Map<String, Object> metadata = (Map<String, Object>) emf_map.get("_aws");

        assertFalse(metadata.containsKey("LogGroupName"));
        assertFalse(metadata.containsKey("LogStreamName"));
    }

    @Test
    public void testFailuresAreRetried() throws InvalidMetricException {
        // arrange
        Fixture fixture = new Fixture();
        fixture.client.messagesToFail = Constants.MAX_ATTEMPTS_PER_MESSAGE - 1;
        AgentSink sink =
                new AgentSink(
                        "",
                        null,
                        Endpoint.DEFAULT_TCP_ENDPOINT,
                        fixture.factory,
                        1,
                        InstantRetryStrategy::new);

        MetricsContext mc = new MetricsContext();
        mc.putMetric("Time", 10);

        // act
        sink.accept(mc);
        sink.shutdown().join();

        // assert
        assertEquals(Constants.MAX_ATTEMPTS_PER_MESSAGE - 1, fixture.client.messagesFailed);
        assertEquals(1, fixture.client.messagesSent);
    }

    @Test
    public void testFailuresAreRetriedWithMaximumLimit() throws InvalidMetricException {
        // arrange
        Fixture fixture = new Fixture();
        fixture.client.messagesToFail = Constants.MAX_ATTEMPTS_PER_MESSAGE + 1;
        AgentSink sink =
                new AgentSink(
                        "",
                        null,
                        Endpoint.DEFAULT_TCP_ENDPOINT,
                        fixture.factory,
                        1,
                        InstantRetryStrategy::new);

        MetricsContext mc = new MetricsContext();
        mc.putMetric("Time", 10);

        // act
        sink.accept(mc);
        sink.shutdown().join();

        // assert
        assertEquals(Constants.MAX_ATTEMPTS_PER_MESSAGE, fixture.client.messagesFailed);
        assertEquals(0, fixture.client.messagesSent);
    }

    @Test
    public void failedMessagesAreQueued() throws InvalidMetricException {
        // arrange
        Fixture fixture = new Fixture();
        fixture.client.messagesToFail = Constants.MAX_ATTEMPTS_PER_MESSAGE * 2;
        AgentSink sink =
                new AgentSink(
                        "",
                        null,
                        Endpoint.DEFAULT_TCP_ENDPOINT,
                        fixture.factory,
                        1,
                        InstantRetryStrategy::new);

        MetricsContext mc = new MetricsContext();
        mc.putMetric("Time", 10);

        // act
        sink.accept(mc);
        sink.accept(mc);

        sink.shutdown().join();

        // assert
        assertEquals(Constants.MAX_ATTEMPTS_PER_MESSAGE * 2, fixture.client.messagesFailed);
        assertEquals(0, fixture.client.messagesSent);
    }

    @Test
    public void queuedMessagesAreBounded() throws InvalidMetricException {
        // arrange
        Fixture fixture = new Fixture();
        fixture.client.messagesToFail = Constants.MAX_ATTEMPTS_PER_MESSAGE * 3;
        AgentSink sink =
                new AgentSink(
                        "",
                        null,
                        Endpoint.DEFAULT_TCP_ENDPOINT,
                        fixture.factory,
                        2,
                        InstantRetryStrategy::new);

        MetricsContext mc = new MetricsContext();
        mc.putMetric("Time", 10);

        // act
        sink.accept(mc);
        sink.accept(mc);

        sink.shutdown().join();

        // assert
        assertEquals(Constants.MAX_ATTEMPTS_PER_MESSAGE * 2, fixture.client.messagesFailed);
        assertEquals(0, fixture.client.messagesSent);
    }

    @Test
    public void oldestMessagesAreDropped() throws InvalidMetricException {
        // arrange
        Fixture fixture = new Fixture();
        AgentSink sink =
                new AgentSink(
                        "",
                        null,
                        Endpoint.DEFAULT_TCP_ENDPOINT,
                        fixture.factory,
                        1,
                        InstantRetryStrategy::new);

        // prevent any message from being sent by the client yet
        fixture.client.lock.lock();

        // two different payloads
        // we'll fill the queue with the first one and then insert the second
        MetricsContext send = new MetricsContext();
        send.putMetric("SEND", 10);

        MetricsContext shouldDrop = new MetricsContext();
        shouldDrop.putMetric("DROP", 10);

        // act
        sink.accept(send);
        sink.accept(
                shouldDrop); // this goes in second because the first message will be pulled off the
        // queue immediately
        sink.accept(send); // this one should overwrite the previous message
        fixture.client.lock.unlock();
        sink.shutdown().join();

        // assert
        assertEquals(0, fixture.client.messagesFailed);
        assertEquals(2, fixture.client.messagesSent);
        fixture.client.messages.forEach(message -> assertFalse(message.contains("DONT_SEND")));
    }

    @Test
    public void cannotEnqueueDataAfterShuttingDownSink() {
        // arrange
        Fixture fixture = new Fixture();
        AgentSink sink =
                new AgentSink(
                        "",
                        null,
                        Endpoint.DEFAULT_TCP_ENDPOINT,
                        fixture.factory,
                        1,
                        InstantRetryStrategy::new);

        // act
        sink.shutdown();

        // assert
        assertThrows(EMFClientException.class, () -> sink.accept(new MetricsContext()));
    }

    class Fixture {
        SocketClientFactory factory;
        TestClient client;

        Fixture() {
            factory = mock(SocketClientFactory.class);
            client = new TestClient();
            when(factory.getClient(any())).thenReturn(client);
        }
    }

    class TestClient implements SocketClient {

        private final ArrayList<String> messages = new ArrayList<>();

        // use this lock to control concurrency and block writing / retires
        // to the socket
        private final ReentrantLock lock = new ReentrantLock();

        private int messagesSent = 0;
        private int messagesFailed = 0;
        private int messagesToFail = 0;

        @Override
        public void sendMessage(String message) {
            if (messagesToFail > messagesFailed) {
                messagesFailed++;
                throw new RuntimeException("Failed to send message");
            } else {
                messagesSent++;
                lock.lock();
                this.messages.add(message);
                lock.unlock();
            }
        }

        public ArrayList<String> getMessages() {
            return this.messages;
        }

        @Override
        public void close() {}
    }

    class InstantRetryStrategy implements RetryStrategy {

        @Override
        public int next() {
            return 0;
        }
    }
}
