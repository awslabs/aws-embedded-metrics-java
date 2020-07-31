package software.amazon.awssdk.services.cloudwatchlogs.emf.config;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.Environments;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemWrapper.class})
public class EnvironmentConfigurationProviderTest {

    @Test
    public void getGetConfig() {
        PowerMockito.mockStatic(SystemWrapper.class);

        putEnv("AWS_EMF_SERVICE_NAME", "TestServiceName");
        putEnv("AWS_EMF_SERVICE_TYPE", "TestServiceType");
        putEnv("AWS_EMF_LOG_GROUP_NAME", "TestLogGroup");
        putEnv("AWS_EMF_LOG_STREAM_NAME", "TestLogStream");
        putEnv("AWS_EMF_AGENT_ENDPOINT", "Endpoint");
        putEnv("AWS_EMF_ENVIRONMENT", "Agent");
        Configuration config = EnvironmentConfigurationProvider.getConfig();

        assertEquals(config.getServiceName().get(), "TestServiceName");
        assertEquals(config.getServiceType().get(), "TestServiceType");
        assertEquals(config.getLogGroupName().get(), "TestLogGroup");
        assertEquals(config.getLogStreamName().get(), "TestLogStream");
        assertEquals(config.agentEndpoint.get(), "Endpoint");
        assertEquals(config.getEnvironmentOverride(), Environments.Agent);
    }

    private void putEnv(String key, String value) {
        PowerMockito.when(SystemWrapper.getenv(key)).thenReturn(value);
    }
}
