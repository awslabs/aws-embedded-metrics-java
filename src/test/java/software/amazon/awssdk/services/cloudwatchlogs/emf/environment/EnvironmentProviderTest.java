package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import static org.junit.Assert.assertTrue;

import com.github.javafaker.Faker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.SystemWrapper;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemWrapper.class})
public class EnvironmentProviderTest {
    private EnvironmentProvider environmentProvider;
    private Faker faker = new Faker();

    @Before
    public void setUp() {
        environmentProvider = new EnvironmentProvider();
    }

    @Test
    public void testResolveEnvironmentReturnsLambdaEnvironment() {
        environmentProvider.cleanResolvedEnvironment();
        String lambdaFunctionName = faker.name().name();

        PowerMockito.mockStatic(SystemWrapper.class);
        PowerMockito.when(SystemWrapper.getenv("AWS_LAMBDA_FUNCTION_NAME"))
                .thenReturn(lambdaFunctionName);

        Environment resolvedEnvironment = environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment instanceof LambdaEnvironment);
    }

    @Test
    public void testResolveEnvironmentReturnsDefaultEnvironment() {
        environmentProvider.cleanResolvedEnvironment();

        Environment resolvedEnvironment = environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment instanceof DefaultEnvironment);
    }
}
