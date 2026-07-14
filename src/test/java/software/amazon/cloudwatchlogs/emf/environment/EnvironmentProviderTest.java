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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;
import static org.powermock.api.mockito.PowerMockito.when;

import com.github.javafaker.Faker;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
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
    public void testResolveEnvironmentFromCacheAfterSecondCall() {
        environmentProvider.cleanResolvedEnvironment();
        String lambdaFunctionName = faker.name().name();

        PowerMockito.mockStatic(SystemWrapper.class);
        when(SystemWrapper.getenv("AWS_LAMBDA_FUNCTION_NAME")).thenReturn(lambdaFunctionName);

        // First call
        CompletableFuture<Environment> resolvedEnvironment =
                environmentProvider.resolveEnvironment();

        assertTrue(resolvedEnvironment.join() instanceof LambdaEnvironment);

        // Second call
        assertTrue(environmentProvider.resolveEnvironment().join() instanceof LambdaEnvironment);

        // The SystemWrapper.getenv should only be called once due to cache
        PowerMockito.verifyStatic(SystemWrapper.class);
        SystemWrapper.getenv("AWS_LAMBDA_FUNCTION_NAME");
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

    @Test
    public void testResolveEnvironmentResolvesOnceUnderConcurrentCallers() throws Exception {
        environmentProvider.cleanResolvedEnvironment();

        /*
        EnvironmentProvider.resolveEnvironment() tries to resolve a new Environment and produces a call to
        Environment.probe() only when it detects that the static cachedEnvironment is not assigned. This test confirms
        only a single call to Environment.probe() is made in a concurrent access scenario - the first call to
        EnvironmentProvider.resolveEnvironment() resolves the new Environment, and subsequent calls return the cached
        version and perform no resolution.

        We arbitrarily set up 8 concurrent thread calls to environmentProvider.resolveEnvironment(). We guarantee that
        all threads passed through resolveEnvironment() and produced a CompletableFuture (either via cache or actual
        resolution) by gating on an 'allCallersEntered' CountDownLatch.  Any thread that is trying to resolve the
        environment via probe() is paused using a 'releaseResolution' CountDownLatch, so that the cachedEnvironment is
        guaranteed to be unpopulated when callers might race against it.

        Our principal assertions are that only one thread entered probe() (verified via the probeCount AtomicInteger)
        and that the Environment returned by all threads is the same.
         */

        int threadCount = 8;
        AtomicInteger probeCount = new AtomicInteger(0);
        CountDownLatch releaseResolution = new CountDownLatch(1);
        CountDownLatch allCallersEntered = new CountDownLatch(threadCount);

        Environment candidate = mock(Environment.class);
        // probe() runs on a background pool thread (via CompletableFuture.supplyAsync inside the
        // provider), so blocking on releaseResolution holds the resolution in flight without
        // stalling the callers below.
        when(candidate.probe())
                .thenAnswer(
                        invocation -> {
                            probeCount.incrementAndGet();
                            assertTrue(releaseResolution.await(10, TimeUnit.SECONDS));
                            return true;
                        });

        // Swap the provider's real environments (Lambda/ECS/EC2/Default) for our single mock, so a
        // resolution probes exactly this candidate.
        FieldSetter.setField(
                environmentProvider,
                EnvironmentProvider.class.getDeclaredField("environments"),
                new Environment[] {candidate});

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        try {
            // Future<CompletableFuture<Environment>>: the outer Future completes once the worker
            // has called resolveEnvironment(); the inner CompletableFuture is the resolution
            // result.
            List<Future<CompletableFuture<Environment>>> submissions = new ArrayList<>();
            for (int i = 0; i < threadCount; i++) {
                submissions.add(
                        pool.submit(
                                () -> {
                                    CompletableFuture<Environment> future =
                                            environmentProvider.resolveEnvironment();
                                    allCallersEntered.countDown();
                                    return future;
                                }));
            }

            // All callers have now raced the still-empty cache; release the held resolution.
            assertTrue(allCallersEntered.await(10, TimeUnit.SECONDS));
            releaseResolution.countDown();

            Environment resolved = submissions.get(0).get(10, TimeUnit.SECONDS).join();
            for (Future<CompletableFuture<Environment>> submission : submissions) {
                assertSame(resolved, submission.get(10, TimeUnit.SECONDS).join());
            }
            assertSame(candidate, resolved);
            assertEquals(1, probeCount.get());
        } finally {
            // Unblock any still-parked probe() in case an assertion failed before we released
            // (countDown() on a zeroed latch is a no-op), then stop the pool.
            releaseResolution.countDown();
            pool.shutdownNow();
        }
    }
}
