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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.internal.util.reflection.FieldSetter;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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

    @After
    public void cleanCache() {
        environmentProvider.cleanResolvedEnvironment();
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

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof LambdaEnvironment);
    }

    @Test
    public void testResolveEnvironmentReturnsDefaultEnvironment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.Agent);

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof DefaultEnvironment);
    }

    @Test
    public void testResolveEnvironmentReturnsEC2Environment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.EC2);

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof EC2Environment);
    }

    @Test
    public void testResolveEnvironmentReturnsECSEnvironment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.ECS);

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof ECSEnvironment);
    }

    @Test
    public void testResolveEnvironmentReturnsLocalEnvironment() {
        PowerMockito.mockStatic(EnvironmentConfigurationProvider.class);
        when(EnvironmentConfigurationProvider.getConfig()).thenReturn(config);
        when(config.getEnvironmentOverride()).thenReturn(Environments.Local);

        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof LocalEnvironment);
    }

    @Test
    public void testResolveEnvironmentEC2AndECSEnvs() throws Exception {
        ECSEnvironment mockedECSEnv = mock(ECSEnvironment.class);
        when(mockedECSEnv.probe()).thenReturn(true);
        EC2Environment mockedEC2Env = mock(EC2Environment.class);
        when(mockedEC2Env.probe()).thenReturn(true);
        DefaultEnvironment mockedDefaultEnv = mock(DefaultEnvironment.class);
        when(mockedDefaultEnv.probe()).thenReturn(true);

        Environment[] envs = new Environment[] {mockedECSEnv, mockedEC2Env, mockedDefaultEnv};
        FieldSetter.setField(
                environmentProvider,
                EnvironmentProvider.class.getDeclaredField("environments"),
                envs);
        Environment env = environmentProvider.resolveEnvironment().join();
        assertSame(env, mockedECSEnv);
        environmentProvider.cleanResolvedEnvironment();

        Environment[] EC2FirstEnvs =
                new Environment[] {mockedEC2Env, mockedECSEnv, mockedDefaultEnv};
        FieldSetter.setField(
                environmentProvider,
                EnvironmentProvider.class.getDeclaredField("environments"),
                EC2FirstEnvs);
        Environment expectedEnv = environmentProvider.resolveEnvironment().join();
        assertSame(expectedEnv, mockedEC2Env);
        environmentProvider.cleanResolvedEnvironment();
    }

    @Test
    public void testResolveEnvironmentReturnFirstDetectedEnvironment() throws Exception {

        long startTime = System.currentTimeMillis();
        LambdaEnvironment mockedLambdaEnv = mock(LambdaEnvironment.class);
        when(mockedLambdaEnv.probe()).thenReturn(true);
        EC2Environment mockedEC2Env = mock(EC2Environment.class);
        when(mockedEC2Env.probe()).thenReturn(true);
        DefaultEnvironment mockedDefaultEnv = mock(DefaultEnvironment.class);
        when(mockedDefaultEnv.probe())
                .thenAnswer(
                        new Answer<Boolean>() {
                            @Override
                            public Boolean answer(InvocationOnMock invocation) throws Throwable {
                                Thread.sleep(5_000);
                                return true;
                            }
                        });
        Environment[] envs = new Environment[] {mockedLambdaEnv, mockedDefaultEnv, mockedEC2Env};

        FieldSetter.setField(
                environmentProvider,
                EnvironmentProvider.class.getDeclaredField("environments"),
                envs);
        Environment env = environmentProvider.resolveEnvironment().join();
        assertSame(env, mockedLambdaEnv);
        assertTrue(System.currentTimeMillis() - startTime < 3_000);
    }
}
