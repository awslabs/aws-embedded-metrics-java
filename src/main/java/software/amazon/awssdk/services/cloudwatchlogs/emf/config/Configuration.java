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

package software.amazon.awssdk.services.cloudwatchlogs.emf.config;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.Environments;

/** Configuration for EMF logger. */
@AllArgsConstructor
public class Configuration {
    /** Whether or not internal logging should be enabled. */
    @Getter @Setter boolean debuggingLoggingEnabled;

    /** The name of the service to use in the default dimensions. */
    @Getter @Setter Optional<String> serviceName;

    /** The type of the service to use in the default dimensions. */
    @Getter @Setter Optional<String> serviceType;

    /**
     * The LogGroup name to use. This is only used for the Cloudwatch Agent in agent-based
     * environment.
     */
    @Getter @Setter Optional<String> logGroupName;

    /** The LogStream name to use. This will be ignored when using the Lambda scope. */
    @Getter @Setter Optional<String> logStreamName;

    /** The endpoint to use to connect to the CloudWatch Agent. */
    @Getter @Setter Optional<String> agentEndpoint;

    /**
     * Environment override. This will short circuit auto-environment detection. Valid values
     * include: - Local: no decoration and sends over stdout - Lambda: decorates logs with Lambda
     * metadata and sends over stdout - Agent: no decoration and sends over TCP - EC2: decorates
     * logs with EC2 metadata and sends over TCP
     */
    @Getter @Setter Environments environmentOverride;
}
