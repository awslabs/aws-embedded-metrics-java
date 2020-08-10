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

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.ConsoleSink;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemWrapper.class})
public class LambdaEnvironmentTest {
    private Faker faker = new Faker();
    private LambdaEnvironment lambda;

    @Before
    public void setUp() {
        lambda = new LambdaEnvironment();
        PowerMockito.mockStatic(SystemWrapper.class);
    }

    @Test
    public void testGetNameReturnFunctionsName() {
        String expectedName = faker.name().name();
        when(SystemWrapper.getenv("AWS_LAMBDA_FUNCTION_NAME")).thenReturn(expectedName);

        assertEquals(lambda.getName(), expectedName);
    }

    @Test
    public void testGetTypeReturnCFNLambdaName() {
        assertEquals(lambda.getType(), "AWS::Lambda::Function");
    }

    @Test
    public void testGetLogGroupNameReturnFunctionName() {
        String expectedName = faker.name().name();
        when(SystemWrapper.getenv("AWS_LAMBDA_FUNCTION_NAME")).thenReturn(expectedName);

        assertEquals(lambda.getLogGroupName(), expectedName);
    }

    @Test
    public void testConfigureContextAddProperties() {
        MetricsContext mc = new MetricsContext();

        String expectedEnv = faker.name().name();
        when(SystemWrapper.getenv("AWS_EXECUTION_ENV")).thenReturn(expectedEnv);

        String expectedVersion = faker.number().digit();
        when(SystemWrapper.getenv("AWS_LAMBDA_FUNCTION_VERSION")).thenReturn(expectedVersion);

        String expectedLogName = faker.name().name();
        when(SystemWrapper.getenv("AWS_LAMBDA_LOG_STREAM_NAME")).thenReturn(expectedLogName);

        lambda.configureContext(mc);

        assertEquals(mc.getProperty("executionEnvironment"), expectedEnv);
        assertEquals(mc.getProperty("functionVersion"), expectedVersion);
        assertEquals(mc.getProperty("logStreamId"), expectedLogName);
        assertNull(mc.getProperty("traceId"));
    }

    @Test
    public void testContextWithTraceId() {
        MetricsContext mc = new MetricsContext();

        String expectedTraceId = "Sampled=1;Count=1";
        when(SystemWrapper.getenv("_X_AMZN_TRACE_ID")).thenReturn(expectedTraceId);

        lambda.configureContext(mc);

        assertEquals(mc.getProperty("traceId"), expectedTraceId);
    }

    @Test
    public void testTraceIdWithOhterSampledValue() {
        MetricsContext mc = new MetricsContext();

        String expectedTraceId = "Sampled=0;Count=1";
        when(SystemWrapper.getenv("_X_AMZN_TRACE_ID")).thenReturn(expectedTraceId);

        lambda.configureContext(mc);

        assertNull(mc.getProperty("traceId"));
    }

    @Test
    public void getCreateSinkReturnsLambdaSink() {
        assertTrue(lambda.getSink() instanceof ConsoleSink);
    }
}
