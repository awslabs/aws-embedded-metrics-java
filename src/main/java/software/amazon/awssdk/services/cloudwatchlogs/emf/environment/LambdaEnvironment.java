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

import java.util.Optional;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;

/** An environment stands for the AWS Lambda environment. */
class LambdaEnvironment implements Environment {
    private static final String AWS_EXECUTION_ENV = "AWS_EXECUTION_ENV";
    private static final String LAMBDA_FUNCTION_NAME = "AWS_LAMBDA_FUNCTION_NAME";
    private static final String LAMBDA_FUNCTION_VERSION = "AWS_LAMBDA_FUNCTION_VERSION";
    private static final String LAMBDA_LOG_STREAM = "AWS_LAMBDA_LOG_STREAM_NAME";
    private static final String TRACE_ID = "_X_AMZN_TRACE_ID";
    private static final String LAMBDA_CFN_NAME = "AWS::Lambda::Function";

    private ISink sink = null;

    // TODO: support probing asynchronously
    @Override
    public boolean probe() {
        String functionName = getEnv(LAMBDA_FUNCTION_NAME);
        return functionName != null;
    }

    @Override
    public String getName() {
        String functionName = getEnv(LAMBDA_FUNCTION_NAME);
        return functionName != null ? functionName : "Unknown";
    }

    @Override
    public String getType() {
        return LAMBDA_CFN_NAME;
    }

    @Override
    public String getLogGroupName() {
        return getName();
    }

    @Override
    public void configureContext(MetricsContext context) {
        addProperty(context, "executionEnvironment", getEnv(AWS_EXECUTION_ENV));
        addProperty(context, "functionVersion", getEnv(LAMBDA_FUNCTION_VERSION));
        addProperty(context, "logStreamId", getEnv(LAMBDA_LOG_STREAM));
        getSampledTrace().ifPresent(traceId -> addProperty(context, "traceId", traceId));
    }

    @Override
    public ISink getSink() {
        if (sink == null) {
            sink = new ConsoleSink();
        }
        return sink;
    }

    private void addProperty(MetricsContext context, String key, String value) {
        if (value != null) {
            context.putProperty(key, value);
        }
    }

    private Optional<String> getSampledTrace() {
        String traceId = getEnv(TRACE_ID);
        if (traceId != null && traceId.contains("Sampled=1")) {
            return Optional.of(traceId);
        }
        return Optional.empty();
    }

    private String getEnv(String name) {
        return SystemWrapper.getenv(name);
    }
}
