package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.Constants;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ConsoleSink;

public class LocalEnvironmentTest {
    private LocalEnvironment environment;
    private Configuration config;
    private Faker faker = new Faker();

    @Before
    public void setUp() {
        config = mock(Configuration.class);
        environment = new LocalEnvironment(config);
    }

    @Test
    public void testProbeReturnFalse() {

        assertFalse(environment.probe());
    }

    @Test
    public void testGetName() {
        when(config.getServiceName()).thenReturn(Optional.empty());
        assertEquals(environment.getName(), Constants.UNKNOWN);

        String name = faker.letterify("?????");
        when(config.getServiceName()).thenReturn(Optional.of(name));
        assertEquals(environment.getName(), name);
    }

    @Test
    public void testGetType() {
        when(config.getServiceType()).thenReturn(Optional.empty());
        assertEquals(environment.getType(), Constants.UNKNOWN);

        String type = faker.letterify("?????");
        when(config.getServiceType()).thenReturn(Optional.of(type));
        assertEquals(environment.getType(), type);
    }

    @Test
    public void testGetLogGroupName() {
        when(config.getLogGroupName()).thenReturn(Optional.empty());
        assertEquals(environment.getLogGroupName(), Constants.UNKNOWN + "-metrics");

        when(config.getLogGroupName()).thenReturn(Optional.empty());
        String serviceName = faker.letterify("?????");
        when(config.getServiceName()).thenReturn(Optional.of(serviceName));
        assertEquals(environment.getLogGroupName(), serviceName + "-metrics");

        String logGroupName = faker.letterify("?????");
        when(config.getLogGroupName()).thenReturn(Optional.of(logGroupName));
        assertEquals(environment.getLogGroupName(), logGroupName);
    }

    @Test
    public void testGetSink() {

        assertTrue(environment.getSink() instanceof ConsoleSink);
        assertSame(environment.getSink(), environment.getSink());
    }
}
