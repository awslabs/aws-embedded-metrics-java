package software.amazon.cloudwatchlogs.emf.environment;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.AgentSink;
import software.amazon.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.cloudwatchlogs.emf.sinks.Endpoint;
import software.amazon.cloudwatchlogs.emf.sinks.ISink;

import static org.junit.Assert.assertEquals;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(PowerMockRunner.class)
@PrepareForTest( {SystemWrapper.class, AgentBasedEnvironment.class} )
public class AgentBasedEnvironmentTest {
    public static class AgentBasedEnvironmentTestImplementation extends AgentBasedEnvironment {
        protected AgentBasedEnvironmentTestImplementation(Configuration config) { super(config); }
        @Override
        public boolean probe() { return false; }
        @Override
        public String getType() { return null; }
        @Override
        public void configureContext(MetricsContext context) { }
    }

    private Configuration configuration;

    @Before
    public void setup() {
        this.configuration = new Configuration();
    }

    @Test
    public void testGetSinkWithDefaultEndpoint() throws Exception {
        AgentSink mockedSink = mock(AgentSink.class);
        PowerMockito.whenNew(AgentSink.class).withAnyArguments().then(invocation -> {
            Endpoint endpoint = invocation.getArgument(2);
            assertEquals(Endpoint.DEFAULT_TCP_ENDPOINT, endpoint);
            return mockedSink;
        });

        AgentBasedEnvironment env = new AgentBasedEnvironmentTestImplementation(configuration);
        ISink sink = env.getSink();

        assertEquals(mockedSink, sink);
    }

    @Test
    public void testGetSinkWithConfiguredEndpoint() throws Exception {
        String endpointUrl = "http://configured-endpoint:1234";
        configuration.setAgentEndpoint(endpointUrl);
        AgentSink mockedSink = mock(AgentSink.class);
        PowerMockito.whenNew(AgentSink.class).withAnyArguments().then(invocation -> {
            Endpoint endpoint = invocation.getArgument(2);
            assertEquals(Endpoint.fromURL(endpointUrl), endpoint);
            return mockedSink;
        });

        AgentBasedEnvironment env = new AgentBasedEnvironmentTestImplementation(configuration);
        ISink sink = env.getSink();

        assertEquals(mockedSink, sink);
    }

    @Test
    public void testGetSinkOverrideToStdOut() {
        configuration.setShouldWriteToStdout(true);

        AgentBasedEnvironment env = new AgentBasedEnvironmentTestImplementation(configuration);
        ISink sink = env.getSink();

        assertEquals(ConsoleSink.class, sink.getClass());
    }

    @Test
    public void testGetSinkOverrideToStdOutFailFastOnImproperOverride() throws Exception {
        configuration.setShouldWriteToStdout(false);

        testGetSinkWithDefaultEndpoint();
    }
}
