package software.amazon.awssdk.services.cloudwatchlogs.emf.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.github.javafaker.Faker;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.Environments;

public class ConfigurationTest {
    private Configuration config;
    private Faker faker;

    @Before
    public void setUp() {
        config = new Configuration();
        faker = new Faker();
    }

    @Test
    public void testReturnEmptyIfNotSet() {
        assertFalse(config.getAgentEndpoint().isPresent());
        assertFalse(config.getLogGroupName().isPresent());
        assertFalse(config.getLogStreamName().isPresent());
        assertFalse(config.getServiceType().isPresent());
        assertFalse(config.getServiceName().isPresent());

        assertEquals(config.getEnvironmentOverride(), Environments.Unknown);
    }

    @Test
    public void testReturnEmptyIfStringValueIsBlank() {
        config.setAgentEndpoint("");
        config.setLogGroupName("");
        config.setLogStreamName("");
        config.setServiceType("");
        config.setServiceName("");
        config.setEnvironmentOverride(null);

        assertFalse(config.getAgentEndpoint().isPresent());
        assertFalse(config.getLogGroupName().isPresent());
        assertFalse(config.getLogStreamName().isPresent());
        assertFalse(config.getServiceType().isPresent());
        assertFalse(config.getServiceName().isPresent());
        assertEquals(config.getEnvironmentOverride(), Environments.Unknown);
    }

    @Test
    public void testReturnCorrectValueAfterSet() {
        String expectedEndpoint = faker.letterify("????");
        String expectedLogGroupName = faker.letterify("????");
        String expectedLogStreamName = faker.letterify("????");
        String expectedServiceType = faker.letterify("????");
        String expectedServiceName = faker.letterify("????");
        Environments expectedEnvironment = Environments.Agent;
        config.setAgentEndpoint(expectedEndpoint);
        config.setLogGroupName(expectedLogGroupName);
        config.setLogStreamName(expectedLogStreamName);
        config.setServiceType(expectedServiceType);
        config.setServiceName(expectedServiceName);
        config.setEnvironmentOverride(expectedEnvironment);

        assertEquals(config.getAgentEndpoint().get(), expectedEndpoint);
        assertEquals(config.getLogGroupName().get(), expectedLogGroupName);
        assertEquals(config.getLogStreamName().get(), expectedLogStreamName);
        assertEquals(config.getServiceType().get(), expectedServiceType);
        assertEquals(config.getServiceName().get(), expectedServiceName);
        assertEquals(config.getEnvironmentOverride(), expectedEnvironment);
    }
}
