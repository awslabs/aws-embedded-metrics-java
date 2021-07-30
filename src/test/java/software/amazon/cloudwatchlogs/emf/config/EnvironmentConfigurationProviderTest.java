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

package software.amazon.cloudwatchlogs.emf.config;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.cloudwatchlogs.emf.environment.Environments;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemWrapper.class})
public class EnvironmentConfigurationProviderTest {

    @Test
    public void getGetConfig() {
        PowerMockito.mockStatic(SystemWrapper.class);

        putEnv("AWS_EMF_SERVICE_NAME", "TestServiceName");
        putEnv("AWS_EMF_SERVICE_TYPE", "TestServiceType");
        putEnv("AWS_EMF_LOG_GROUP_NAME", "TestLogGroup");
        putEnv("AWS_EMF_LOG_STREAM_NAME", "TestLogStream");
        putEnv("AWS_EMF_AGENT_ENDPOINT", "Endpoint");
        putEnv("AWS_EMF_ENVIRONMENT", "Agent");
        putEnv("AWS_EMF_ASYNC_BUFFER_SIZE", "9999");

        Configuration config = EnvironmentConfigurationProvider.createConfig();

        assertEquals(config.getServiceName().get(), "TestServiceName");
        assertEquals(config.getServiceType().get(), "TestServiceType");
        assertEquals(config.getLogGroupName().get(), "TestLogGroup");
        assertEquals(config.getLogStreamName().get(), "TestLogStream");
        assertEquals(config.getAgentEndpoint().get(), "Endpoint");
        assertEquals(config.getEnvironmentOverride(), Environments.Agent);
        assertEquals(config.getAsyncBufferSize(), 9999);
    }

    @Test
    public void invalidEnvironmentValuesFallbackToExpectedDefaults() {
        // arrange
        PowerMockito.mockStatic(SystemWrapper.class);

        // act
        putEnv("AWS_EMF_ASYNC_BUFFER_SIZE", "NaN");

        // assert
        Configuration config = EnvironmentConfigurationProvider.createConfig();
        assertEquals(100, config.getAsyncBufferSize());
    }

    private void putEnv(String key, String value) {
        PowerMockito.when(SystemWrapper.getenv(key)).thenReturn(value);
    }
}
