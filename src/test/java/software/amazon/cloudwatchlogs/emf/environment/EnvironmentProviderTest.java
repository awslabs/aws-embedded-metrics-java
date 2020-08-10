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

package software.amazon.cloudwatchlogs.emf.environment;

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
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;

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

    @Test
    public void testResolveEnvironmentReturnsLocalEnvironment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.Local);

        environmentProvider.cleanResolvedEnvironment();

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof LocalEnvironment);
    }
}
