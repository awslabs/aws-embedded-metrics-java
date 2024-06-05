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

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.cloudwatchlogs.emf.sinks.AgentSink;
import software.amazon.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.cloudwatchlogs.emf.sinks.Endpoint;
import software.amazon.cloudwatchlogs.emf.sinks.ISink;
import software.amazon.cloudwatchlogs.emf.sinks.SocketClientFactory;
import software.amazon.cloudwatchlogs.emf.sinks.retry.FibonacciRetryStrategy;

@Slf4j
public abstract class AgentBasedEnvironment implements Environment {
    private final Configuration config;
    private ISink sink;

    protected AgentBasedEnvironment(Configuration config) {
        this.config = config;
    }

    @Override
    public String getName() {
        Optional<String> serviceName = config.getServiceName();

        if (serviceName.isPresent()) {
            return serviceName.get();
        }

        log.warn("Unknown ServiceName.");
        return Constants.UNKNOWN;
    }

    @Override
    public String getLogGroupName() {
        if (config.getLogGroupName().isPresent()) {
            return config.getLogGroupName().get();
        } else {
            String serviceName = getName();
            // for ECS services, replace "repo:tag" format with "repo-tag" to satisfy
            // log group regex
            serviceName = serviceName.replace(":", "-");
            return serviceName + "-metrics";
        }
    }

    public String getLogStreamName() {
        return config.getLogStreamName().orElse("");
    }

    @Override
    public ISink getSink() {
        if (sink == null) {
            if (config.shouldWriteToStdout()) {
                sink = new ConsoleSink();
            } else {
                Endpoint endpoint;
                if (config.getAgentEndpoint().isPresent()) {
                    endpoint = Endpoint.fromURL(config.getAgentEndpoint().get());
                } else {
                    log.info(
                        "Endpoint is not defined. Using default: {}",
                        Endpoint.DEFAULT_TCP_ENDPOINT);
                    endpoint = Endpoint.DEFAULT_TCP_ENDPOINT;
                }
                sink =
                    new AgentSink(
                        getLogGroupName(),
                        getLogStreamName(),
                        endpoint,
                        new SocketClientFactory(),
                        config.getAsyncBufferSize(),
                        () ->
                            new FibonacciRetryStrategy(
                                Constants.MIN_BACKOFF_MILLIS,
                                Constants.MAX_BACKOFF_MILLIS,
                                Constants.MAX_BACKOFF_JITTER));
            }
        }
        return sink;
    }
}
