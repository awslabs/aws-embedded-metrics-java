package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;

public class DefaultEnvironmentTest {
    private DefaultEnvironment environment;
    private Configuration configuration;

    @Before
    public void setUp() {
        configuration = mock(Configuration.class);
        environment = new DefaultEnvironment(configuration);
    }

    @Test
    public void testGetName() {
        String serviceName = "TestService";
        when(configuration.getServiceName()).thenReturn(Optional.of(serviceName));
        assertEquals(environment.getName(), serviceName);
    }

    @Test
    public void testGetNameWhenNotConfigured() {
        when(configuration.getServiceName()).thenReturn(Optional.empty());
        assertEquals(environment.getName(), "Unknown");
    }

    @Test
    public void testGetType() {
        String serviceType = "TestServiceType";
        when(configuration.getServiceType()).thenReturn(Optional.of(serviceType));
        assertEquals(environment.getType(), serviceType);
    }

    @Test
    public void testGetTypeWhenNotConfigured() {
        when(configuration.getServiceType()).thenReturn(Optional.empty());
        assertEquals(environment.getType(), "Unknown");
    }

    @Test
    public void testGetLogStreamName() {
        String logStream = "TestLogStream";
        when(configuration.getLogStreamName()).thenReturn(Optional.of(logStream));
        assertEquals(environment.getLogStreamName(), logStream);
    }

    @Test
    public void testGetLogStreamNameWhenNotConfigured() {
        String serviceName = "TestService";
        when(configuration.getLogStreamName()).thenReturn(Optional.empty());
        when(configuration.getServiceName()).thenReturn(Optional.of(serviceName));
        assertEquals(environment.getLogStreamName(), "");
    }

    @Test
    public void testGetLogGroupName() {
        String logGroup = "TestLogGroup";
        when(configuration.getLogGroupName()).thenReturn(Optional.of(logGroup));
        assertEquals(environment.getLogGroupName(), logGroup);
    }

    @Test
    public void testGetLogLogNameWhenNotConfigured() {
        String serviceName = "TestService";
        when(configuration.getLogGroupName()).thenReturn(Optional.empty());
        when(configuration.getServiceName()).thenReturn(Optional.of(serviceName));
        assertEquals(environment.getLogGroupName(), serviceName + "-metrics");
    }
}
