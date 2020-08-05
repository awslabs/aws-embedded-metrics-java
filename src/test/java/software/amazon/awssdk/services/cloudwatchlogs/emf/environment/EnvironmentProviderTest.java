package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import com.github.javafaker.Faker;
import java.util.concurrent.CompletableFuture;
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
    public void testResolveEnvironmentReturnCachedEnv() {
        environmentProvider.cleanResolvedEnvironment();
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.Lambda);

        Environment env = environmentProvider.resolveEnvironment().join();
        assertTrue(env instanceof LambdaEnvironment);
        when(config.getEnvironmentOverride()).thenReturn(Environments.EC2);
        assertSame(env, environmentProvider.resolveEnvironment().join());
    }

    @Test
    public void testResolveEnvironmentReturnsLambdaEnvironment() {
        environmentProvider.cleanResolvedEnvironment();
        String lambdaFunctionName = faker.name().name();

        PowerMockito.mockStatic(SystemWrapper.class);
        when(SystemWrapper.getenv("AWS_LAMBDA_FUNCTION_NAME")).thenReturn(lambdaFunctionName);

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof LambdaEnvironment);
    }

    @Test
    public void testResolveEnvironmentReturnsLambdaFromOverride() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.Lambda);

        environmentProvider.cleanResolvedEnvironment();

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof LambdaEnvironment);
    }

    @Test
    public void testResolveEnvironmentReturnsDefaultEnvironment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.Agent);

        environmentProvider.cleanResolvedEnvironment();

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof DefaultEnvironment);
    }

    @Test
    public void testResolveEnvironmentReturnsEC2Environment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.EC2);

        environmentProvider.cleanResolvedEnvironment();

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof EC2Environment);
    }

    @Test
    public void testResolveEnvironmentReturnsECSEnvironment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.ECS);

        environmentProvider.cleanResolvedEnvironment();

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof ECSEnvironment);
    }
}
