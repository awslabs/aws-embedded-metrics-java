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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SystemWrapper.class })
public class ECSEnvironmentTest {
    private Configuration config;
    private ECSEnvironment environment;
    private ResourceFetcher fetcher;
    private Faker faker = new Faker();

    @Before
    public void setUp() {
        config = mock(Configuration.class);
        fetcher = mock(ResourceFetcher.class);
        environment = new ECSEnvironment(config, fetcher);
    }

    @Test
    public void testProbeReturnFalseIfNoURL() {
        PowerMockito.mockStatic(SystemWrapper.class);
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(null);

        assertFalse(environment.probe());
    }

    @Test
    public void testReturnTrueWithCorrectURL() {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        ECSEnvironment.ECSMetadata metadata = new ECSEnvironment.ECSMetadata();
        when(fetcher.fetch(any(), (ObjectMapper) any(), any())).thenReturn(metadata);

        assertTrue(environment.probe());
    }

    @Test
    public void testFormatImageName() {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        ECSEnvironment.ECSMetadata metadata = new ECSEnvironment.ECSMetadata();
        metadata.image = "testAccount.dkr.ecr.us-west-2.amazonaws.com/testImage:latest";
        metadata.labels = new HashMap<>();
        when(fetcher.fetch(any(), (ObjectMapper) any(), any())).thenReturn(metadata);

        assertTrue(environment.probe());
        assertEquals("testImage:latest", environment.getName());
    }

    @Test
    public void testGetNameFromConfig() {
        String serviceName = "testService";
        when(config.getServiceName()).thenReturn(Optional.of(serviceName));

        assertEquals(serviceName, environment.getName());
    }

    @Test
    public void testGetNameReturnsUnknown() {
        when(config.getServiceName()).thenReturn(Optional.empty());
        assertEquals(Constants.UNKNOWN, environment.getName());
    }

    @Test
    public void testGetType() {
        assertEquals("AWS::ECS::Container", environment.getType());
    }

    @Test
    public void testGetTypeFromConfig() {
        String type = faker.letterify("????");
        when(config.getServiceType()).thenReturn(Optional.of(type));
        assertEquals(type, environment.getType());
    }

    @Test
    public void testSetFluentBit() {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        String fluentHost = "localhost";
        PowerMockito.when(SystemWrapper.getenv("FLUENT_HOST")).thenReturn(fluentHost);

        environment.probe();
        when(fetcher.fetch(any(), (ObjectMapper) any(), any()))
                .thenReturn(new ECSEnvironment.ECSMetadata());
        ArgumentCaptor<String> argument = ArgumentCaptor.forClass(String.class);
        Mockito.verify(config, times(1)).setAgentEndpoint(argument.capture());
        assertEquals("tcp://" + fluentHost + ":" + Constants.DEFAULT_AGENT_PORT, argument.getValue());
    }

    @Test
    public void testGetLogGroupNameReturnEmpty() {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        String fluentHost = "localhost";
        PowerMockito.when(SystemWrapper.getenv("FLUENT_HOST")).thenReturn(fluentHost);

        environment.probe();

        assertEquals("", environment.getLogGroupName());
    }

    @Test
    public void testGetLogGroupNameReturnNonEmpty() {
        assertEquals(Constants.UNKNOWN + "-metrics", environment.getLogGroupName());
    }

    @Test
    public void testGetLogGroupNameReplaceColon() {
        String serviceName = "testRepo:testTag";
        when(config.getServiceName()).thenReturn(Optional.of(serviceName));

        assertEquals(environment.getLogGroupName(), "testRepo-testTag-metrics");
    }

    @Test
    public void testConfigureContext() throws UnknownHostException {
        PowerMockito.mockStatic(SystemWrapper.class);
        String uri = "http://ecs-metata.com";
        PowerMockito.when(SystemWrapper.getenv("ECS_CONTAINER_METADATA_URI")).thenReturn(uri);
        ECSEnvironment.ECSMetadata metadata = new ECSEnvironment.ECSMetadata();
        getRandomMetadata(metadata);
        when(fetcher.fetch(any(), (ObjectMapper) any(), any())).thenReturn(metadata);

        environment.probe();

        MetricsContext context = new MetricsContext();
        environment.configureContext(context);

        assertEquals(InetAddress.getLocalHost().getHostName(), context.getProperty("containerId"));
        assertEquals(metadata.getCreatedAt(), context.getProperty("createdAt"));
        assertEquals(metadata.getStartedAt(), context.getProperty("startedAt"));
        assertEquals(metadata.labels.get("com.amazonaws.ecs.cluster"), context.getProperty("cluster"));
        assertEquals(metadata.labels.get("com.amazonaws.ecs.task-arn"), context.getProperty("taskArn"));
    }

    private void getRandomMetadata(ECSEnvironment.ECSMetadata metadata) {
        metadata.createdAt = faker.date().past(1, TimeUnit.DAYS).toString();
        metadata.startedAt = faker.date().past(1, TimeUnit.DAYS).toString();
        metadata.image = faker.letterify("?????");
        metadata.labels = new HashMap<>();
        metadata.labels.put("com.amazonaws.ecs.cluster", faker.letterify("?????"));
        metadata.labels.put("com.amazonaws.ecs.task-arn", faker.letterify("?????"));
    }
}
