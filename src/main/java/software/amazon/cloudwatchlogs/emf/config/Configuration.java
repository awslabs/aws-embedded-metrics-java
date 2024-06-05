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

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.environment.Environments;
import software.amazon.cloudwatchlogs.emf.util.StringUtils;

/** Configuration for EMF logger. */
@AllArgsConstructor
@NoArgsConstructor
public class Configuration {

    /** The name of the service to use in the default dimensions. */
    @Setter private String serviceName;

    /** The type of the service to use in the default dimensions. */
    @Setter private String serviceType;

    /**
     * The LogGroup name to use. This is only used for the Cloudwatch Agent in agent-based
     * environment.
     */
    @Setter private String logGroupName;

    /** The LogStream name to use. This will be ignored when using the Lambda scope. */
    @Setter private String logStreamName;

    /** The endpoint to use to connect to the CloudWatch Agent. */
    @Setter private String agentEndpoint;

    /**
     * Environment override. This will short circuit auto-environment detection. Valid values
     * include: - Local: no decoration and sends over stdout - Lambda: decorates logs with Lambda
     * metadata and sends over stdout - Agent: no decoration and sends over TCP - EC2: decorates
     * logs with EC2 metadata and sends over TCP
     */
    @Setter Environments environmentOverride;

    /** Queue length for asynchronous sinks. */
    @Setter @Getter int asyncBufferSize = Constants.DEFAULT_ASYNC_BUFFER_SIZE;

    @Setter private boolean shouldWriteToStdout;

    public Optional<String> getServiceName() {
        return getStringOptional(serviceName);
    }

    public Optional<String> getServiceType() {
        return getStringOptional(serviceType);
    }

    public Optional<String> getLogGroupName() {
        return getStringOptional(logGroupName);
    }

    public Optional<String> getLogStreamName() {
        return getStringOptional(logStreamName);
    }

    public Optional<String> getAgentEndpoint() {
        return getStringOptional(agentEndpoint);
    }

    public Environments getEnvironmentOverride() {
        if (environmentOverride == null) {
            return Environments.Unknown;
        }
        return environmentOverride;
    }

    private Optional<String> getStringOptional(String value) {
        if (StringUtils.isNullOrEmpty(value)) {
            return Optional.empty();
        }
        return Optional.of(value);
    }

    public boolean shouldWriteToStdout() {
        return shouldWriteToStdout;
    }
}
