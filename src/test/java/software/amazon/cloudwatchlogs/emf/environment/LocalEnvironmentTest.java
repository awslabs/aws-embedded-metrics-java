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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.sinks.ConsoleSink;

public class LocalEnvironmentTest {
    private LocalEnvironment environment;
    private Configuration config;
    private Faker faker = new Faker();

    @Before
    public void setUp() {
        config = mock(Configuration.class);
        environment = new LocalEnvironment(config);
    }

    @Test
    public void testProbeReturnFalse() {
        assertFalse(environment.probe());
    }

    @Test
    public void testGetName() {
        when(config.getServiceName()).thenReturn(Optional.empty());
        assertEquals(Constants.UNKNOWN, environment.getName());

        String name = faker.letterify("?????");
        when(config.getServiceName()).thenReturn(Optional.of(name));
        assertEquals(name, environment.getName());
    }

    @Test
    public void testGetType() {
        when(config.getServiceType()).thenReturn(Optional.empty());
        assertEquals(Constants.UNKNOWN, environment.getType());

        String type = faker.letterify("?????");
        when(config.getServiceType()).thenReturn(Optional.of(type));
        assertEquals(type, environment.getType());
    }

    @Test
    public void testGetLogGroupName() {
        when(config.getLogGroupName()).thenReturn(Optional.empty());
        assertEquals(Constants.UNKNOWN + "-metrics", environment.getLogGroupName());

        when(config.getLogGroupName()).thenReturn(Optional.empty());
        String serviceName = faker.letterify("?????");
        when(config.getServiceName()).thenReturn(Optional.of(serviceName));
        assertEquals(serviceName + "-metrics", environment.getLogGroupName());

        String logGroupName = faker.letterify("?????");
        when(config.getLogGroupName()).thenReturn(Optional.of(logGroupName));
        assertEquals(logGroupName, environment.getLogGroupName());
    }

    @Test
    public void testGetSink() {
        assertTrue(environment.getSink() instanceof ConsoleSink);
        assertSame(environment.getSink(), environment.getSink());
    }
}
