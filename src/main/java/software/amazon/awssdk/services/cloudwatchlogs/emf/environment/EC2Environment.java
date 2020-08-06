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

package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.Constants;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.exception.EMFClientException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

@Slf4j
class EC2Environment extends AgentBasedEnvironment {
    private Configuration config;
    private EC2Metadata metadata;
    private ResourceFetcher fetcher;

    private static final String INSTANCE_IDENTITY_URL =
            "http://169.254.169.254/latest/dynamic/instance-identity/document";
    private static final String CFN_EC2_TYPE = "AWS::EC2::Instance";

    EC2Environment(Configuration config, ResourceFetcher fetcher) {
        super(config);
        this.config = config;
        this.fetcher = fetcher;
    }

    @Override
    public boolean probe() {
        URI endpoint = null;
        try {
            endpoint = new URI(INSTANCE_IDENTITY_URL);
        } catch (Exception ex) {
            log.debug("Failed to construct url: " + INSTANCE_IDENTITY_URL);
            return false;
        }
        try {
            metadata = fetcher.fetch(endpoint, EC2Metadata.class);
            return true;
        } catch (EMFClientException ex) {
            log.debug("Failed to get response from: " + endpoint, ex);
        }
        return false;
    }

    @Override
    public String getType() {
        if (config.getServiceType().isPresent()) {
            return config.getServiceType().get();
        }
        if (this.metadata != null) {
            return CFN_EC2_TYPE;
        }
        return Constants.UNKNOWN;
    }

    @Override
    public void configureContext(MetricsContext context) {
        if (metadata != null) {
            context.putProperty("imageId", metadata.imageId);
            context.putProperty("instanceId", metadata.instanceId);
            context.putProperty("instanceType", metadata.instanceType);
            context.putProperty("privateIp", metadata.privateIp);
            context.putProperty("availabilityZone", metadata.availabilityZone);
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class EC2Metadata {
        private String imageId;
        private String availabilityZone;
        private String privateIp;
        private String instanceId;
        private String instanceType;
    }
}
