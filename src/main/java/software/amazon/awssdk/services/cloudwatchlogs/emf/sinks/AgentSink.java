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

package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

/** An sink connecting to CloudWatch Agent. */
@Slf4j
public class AgentSink implements ISink {
    private final String logGroupName;
    private final String logStreamName;
    private final SocketClient client;

    public AgentSink(
            String logGroupName,
            String logStreamName,
            Endpoint endpoint,
            SocketClientFactory clientFactory) {
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;
        client = clientFactory.getClient(endpoint);
    }

    public void accept(MetricsContext context) {
        if (logGroupName != null && !logGroupName.isEmpty()) {
            context.putMetadata("LogGroupName", logGroupName);
        }

        if (logStreamName != null && !logStreamName.isEmpty()) {
            context.putMetadata("LogStreamName", logStreamName);
        }

        try {
            client.sendMessage(context.serialize() + "\n");
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize the metrics with the exception: ", e);
        }
    }
}
