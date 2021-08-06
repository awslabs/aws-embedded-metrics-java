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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.github.javafaker.Faker;
import org.junit.Before;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.environment.Environments;

public class ConfigurationTest {
    private Configuration config;
    private Faker faker;

    @Before
    public void setUp() {
        config = new Configuration();
        faker = new Faker();
    }

    @Test
    public void testReturnEmptyOrDefaultIfNotSet() {
        assertFalse(config.getAgentEndpoint().isPresent());
        assertFalse(config.getLogGroupName().isPresent());
        assertFalse(config.getLogStreamName().isPresent());
        assertFalse(config.getServiceType().isPresent());
        assertFalse(config.getServiceName().isPresent());

        assertEquals(config.getEnvironmentOverride(), Environments.Unknown);
        assertEquals(config.getAsyncBufferSize(), 100);
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
        int expectedAsyncBufferSize = faker.number().randomDigit();

        config.setAgentEndpoint(expectedEndpoint);
        config.setLogGroupName(expectedLogGroupName);
        config.setLogStreamName(expectedLogStreamName);
        config.setServiceType(expectedServiceType);
        config.setServiceName(expectedServiceName);
        config.setEnvironmentOverride(expectedEnvironment);
        config.setAsyncBufferSize(expectedAsyncBufferSize);

        assertEquals(config.getAgentEndpoint().get(), expectedEndpoint);
        assertEquals(config.getLogGroupName().get(), expectedLogGroupName);
        assertEquals(config.getLogStreamName().get(), expectedLogStreamName);
        assertEquals(config.getServiceType().get(), expectedServiceType);
        assertEquals(config.getServiceName().get(), expectedServiceName);
        assertEquals(config.getEnvironmentOverride(), expectedEnvironment);
        assertEquals(config.getAsyncBufferSize(), expectedAsyncBufferSize);
    }
}
