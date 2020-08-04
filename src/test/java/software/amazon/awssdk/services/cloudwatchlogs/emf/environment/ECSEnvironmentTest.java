package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.javafaker.Faker;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.services.cloudwatchlogs.emf.Constants;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemWrapper.class})
public class ECSEnvironmentTest {
    private Configuration config;
    private ECSEnvironment environment;
    private ResourceFetcher fetcher;
    private Faker faker = new Faker();

    @Before
    public void setUp() {
        config = mock(Configuration.class);
        fetcher = mock(ResourceFetcher.class);
        environment = new ECSEnvironment(config, fetcher);
    }

    @Test
    public void testProbeReturnFalseIfNoURL() {
        PowerMockito.mockStatic(SystemWrapper.class);
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(null);

        assertFalse(environment.probe());
    }

    @Test
    public void testReturnTrueWithCorrectURL() {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        ECSEnvironment.ECSMetadata metadata = new ECSEnvironment.ECSMetadata();
        when(fetcher.fetch(any(), any(), any())).thenReturn(metadata);

        assertTrue(environment.probe());
    }

    @Test
    public void testFormatImageName() {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        ECSEnvironment.ECSMetadata metadata = new ECSEnvironment.ECSMetadata();
        metadata.image = "testAccount.dkr.ecr.us-west-2.amazonaws.com/testImage:latest";
        metadata.labels = new HashMap<>();
        when(fetcher.fetch(any(), any(), any())).thenReturn(metadata);

        assertTrue(environment.probe());
        assertEquals(environment.getName(), "testImage:latest");
    }

    @Test
    public void testGetNameFromConfig() {
        String serviceName = "testService";
        when(config.getServiceName()).thenReturn(Optional.of(serviceName));

        assertEquals(environment.getName(), serviceName);
    }

    @Test
    public void testGetNameReturnsUnknown() {
        when(config.getServiceName()).thenReturn(Optional.empty());
        assertEquals(environment.getName(), Constants.UNKNOWN);
    }

    @Test
    public void testGetType() {
        assertEquals(environment.getType(), "AWS::ECS::Container");
    }

    @Test
    public void testGetTypeFromConfig() {
        String type = faker.letterify("????");
        when(config.getServiceType()).thenReturn(Optional.of(type));
        assertEquals(environment.getType(), type);
    }

    @Test
    public void testSetFluentBit() {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        String fluentHost = "localhost";
        PowerMockito.when(SystemWrapper.getenv("FLUENT_HOST")).thenReturn(fluentHost);

        environment.probe();
        when(fetcher.fetch(any(), any(), any())).thenReturn(new ECSEnvironment.ECSMetadata());
        ArgumentCaptor<Optional<String>> argument = ArgumentCaptor.forClass(Optional.class);
        Mockito.verify(config, times(1)).setAgentEndpoint(argument.capture());
        assertEquals(
                argument.getValue().get(),
                "tcp://" + fluentHost + ":" + Constants.DEFAULT_AGENT_PORT);
    }

    @Test
    public void testGetLogGroupNameReturnEmpty() {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        String fluentHost = "localhost";
        PowerMockito.when(SystemWrapper.getenv("FLUENT_HOST")).thenReturn(fluentHost);

        environment.probe();

        assertEquals(environment.getLogGroupName(), "");
    }

    @Test
    public void testGetLogGroupNameReturnNonEmpty() {

        assertEquals(environment.getLogGroupName(), Constants.UNKNOWN + "-metrics");
    }

    @Test
    public void testConfigureContext() throws UnknownHostException {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        ECSEnvironment.ECSMetadata metadata = new ECSEnvironment.ECSMetadata();
        getRandomMetadata(metadata);
        when(fetcher.fetch(any(), any(), any())).thenReturn(metadata);

        environment.probe();

        MetricsContext context = new MetricsContext();
        environment.configureContext(context);

        assertEquals(context.getProperty("containerId"), InetAddress.getLocalHost().getHostName());
        assertEquals(context.getProperty("createdAt"), metadata.getCreatedAt());
        assertEquals(context.getProperty("startedAt"), metadata.getStartedAt());
        assertEquals(
                context.getProperty("cluster"), metadata.labels.get("com.amazonaws.ecs.cluster"));
        assertEquals(
                context.getProperty("taskArn"), metadata.labels.get("com.amazonaws.ecs.task-arn"));
    }

    private void getRandomMetadata(ECSEnvironment.ECSMetadata metadata) {

        metadata.createdAt = faker.date().past(1, TimeUnit.DAYS).toString();
        metadata.startedAt = faker.date().past(1, TimeUnit.DAYS).toString();
        metadata.image = faker.letterify("?????");
        metadata.labels = new HashMap<>();
        metadata.labels.put("com.amazonaws.ecs.cluster", faker.letterify("?????"));
        metadata.labels.put("com.amazonaws.ecs.task-arn", faker.letterify("?????"));
    }
}
