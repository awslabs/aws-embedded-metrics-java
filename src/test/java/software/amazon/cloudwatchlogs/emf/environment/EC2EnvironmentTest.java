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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.javafaker.Faker;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.exception.EMFClientException;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SystemWrapper.class})
public class EC2EnvironmentTest {
    private Configuration config;
    private EC2Environment environment;
    private ResourceFetcher fetcher;
    private Faker faker = new Faker();

    @Before
    public void setUp() {
        config = mock(Configuration.class);
        fetcher = mock(ResourceFetcher.class);
        environment = new EC2Environment(config, fetcher);
    }

    @Test
    public void testProbeReturnFalse() {
        when(fetcher.fetch(any(), any())).thenThrow(new EMFClientException("Invalid URL"));

        assertFalse(environment.probe());
    }

    @Test
    public void testProbeReturnTrue() {

        when(fetcher.fetch(any(), any())).thenReturn(new EC2Environment.EC2Metadata());
        assertTrue(environment.probe());
    }

    @Test
    public void testGetTypeWhenNoMetadata() {
        when(fetcher.fetch(any(), any())).thenThrow(new EMFClientException("Invalid URL"));
        environment.probe();
        assertEquals(environment.getType(), Constants.UNKNOWN);
    }

    @Test
    public void testGetTypeReturnDefined() {
        when(fetcher.fetch(any(), any())).thenReturn(new EC2Environment.EC2Metadata());
        environment.probe();
        assertEquals(environment.getType(), "AWS::EC2::Instance");
    }

    @Test
    public void testGetTypeFromConfiguration() {
        when(fetcher.fetch(any(), any())).thenReturn(new EC2Environment.EC2Metadata());
        environment.probe();
        String testType = faker.letterify("???");
        when(config.getServiceType()).thenReturn(Optional.of(testType));
        assertEquals(environment.getType(), testType);
    }

    @Test
    public void testConfigureContext() {
        EC2Environment.EC2Metadata metadata = new EC2Environment.EC2Metadata();
        getRandomMetadata(metadata);
        when(fetcher.fetch(any(), any())).thenReturn(metadata);
        environment.probe();

        MetricsContext context = new MetricsContext();
        environment.configureContext(context);

        assertEquals(context.getProperty("imageId"), metadata.getImageId());
        assertEquals(context.getProperty("instanceId"), metadata.getInstanceId());
        assertEquals(context.getProperty("instanceType"), metadata.getInstanceType());
        assertEquals(context.getProperty("privateIp"), metadata.getPrivateIp());
        assertEquals(context.getProperty("availabilityZone"), metadata.getAvailabilityZone());
    }

    private void getRandomMetadata(EC2Environment.EC2Metadata metadata) {
        metadata.setImageId(faker.letterify("?????"));
        metadata.setInstanceId(faker.letterify("?????"));
        metadata.setInstanceType(faker.letterify("?????"));
        metadata.setPrivateIp(faker.letterify("?????"));
        metadata.setAvailabilityZone(faker.letterify("?????"));
    }
}
