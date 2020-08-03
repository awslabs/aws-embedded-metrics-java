package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.github.javafaker.Faker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.SystemWrapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemWrapper.class, EnvironmentConfigurationProvider.class})
public class EnvironmentProviderTest {
    private EnvironmentProvider environmentProvider;
    private Faker faker = new Faker();
    private Configuration config;

    @Before
    public void setUp() {
        environmentProvider = new EnvironmentProvider();
        config = mock(Configuration.class);
    }

    @Test
    public void testResolveEnvironmentReturnsLambdaEnvironment() {
        environmentProvider.cleanResolvedEnvironment();
        String lambdaFunctionName = faker.name().name();

        PowerMockito.mockStatic(SystemWrapper.class);
        when(SystemWrapper.getenv("AWS_LAMBDA_FUNCTION_NAME")).thenReturn(lambdaFunctionName);

        Environment resolvedEnvironment = environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment instanceof LambdaEnvironment);
    }

    @Test
    public void testResolveEnvironmentReturnsDefaultEnvironment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.Agent);

        environmentProvider.cleanResolvedEnvironment();

        Environment resolvedEnvironment = environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment instanceof DefaultEnvironment);
    }


    @Test
    public void testResolveEnvironmentReturnsEC2Environment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.EC2);

        environmentProvider.cleanResolvedEnvironment();

        Environment resolvedEnvironment = environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment instanceof EC2Environment);
    }

    @Test
    public void testResolveEnvironmentReturnsECSEnvironment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.ECS);

        environmentProvider.cleanResolvedEnvironment();

        Environment resolvedEnvironment = environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment instanceof ECSEnvironment);
    }
}
