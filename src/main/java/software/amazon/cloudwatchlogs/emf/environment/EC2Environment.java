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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.exception.EMFClientException;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

@Slf4j
public class EC2Environment extends AgentBasedEnvironment {
    private Configuration config;
    private EC2Metadata metadata;
    private ResourceFetcher fetcher;

    private static final String INSTANCE_IDENTITY_URL =
            "http://169.254.169.254/latest/dynamic/instance-identity/document";

    private static final String INSTANCE_TOKEN_URL = "http://169.254.169.254/latest/api/token";
    private static final String CFN_EC2_TYPE = "AWS::EC2::Instance";
    private static final String TOKEN_REQUEST_HEADER_KEY = "X-aws-ec2-metadata-token-ttl-seconds";
    private static final String TOKEN_REQUEST_HEADER_VALUE = "21600";

    private static final String METADATA_REQUEST_TOKEN_HEADER_KEY = "X-aws-ec2-metadata-token";

    EC2Environment(Configuration config, ResourceFetcher fetcher) {
        super(config);
        this.config = config;
        this.fetcher = fetcher;
    }

    @Override
    public boolean probe() {
        String token;
        Map<String, String> tokenRequestHeader =
                Collections.singletonMap(TOKEN_REQUEST_HEADER_KEY, TOKEN_REQUEST_HEADER_VALUE);

        URI tokenEndpoint = null;
        try {
            tokenEndpoint = new URI(INSTANCE_TOKEN_URL);
        } catch (Exception ex) {
            log.debug("Failed to construct url: " + INSTANCE_IDENTITY_URL);
            return false;
        }
        try {
            token = fetcher.fetch(tokenEndpoint, "PUT", tokenRequestHeader);
        } catch (EMFClientException ex) {
            log.debug("Failed to get response from: " + tokenEndpoint, ex);
            return false;
        }

        Map<String, String> metadataRequestTokenHeader =
                Collections.singletonMap(METADATA_REQUEST_TOKEN_HEADER_KEY, token);
        URI endpoint = null;
        try {
            endpoint = new URI(INSTANCE_IDENTITY_URL);
        } catch (Exception ex) {
            log.debug("Failed to construct url: " + INSTANCE_IDENTITY_URL);
            return false;
        }
        try {
            metadata =
                    fetcher.fetch(endpoint, "GET", EC2Metadata.class, metadataRequestTokenHeader);
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
