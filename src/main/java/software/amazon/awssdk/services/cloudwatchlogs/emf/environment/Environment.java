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

import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;

/** A runtime environment (e.g. Lambda, EKS, ECS, EC2). */
public interface Environment {

    /** Determines whether or not we are executing in this environment. */
    boolean probe();

    /** Get the environment name. This will be used to set the ServiceName dimension. */
    String getName();

    /** Get the environment type. This will be used to set the ServiceType dimension. */
    String getType();

    /** Get log group name. This will be used to set the LogGroup dimension. */
    String getLogGroupName();

    /**
     * Configure the context with environment properties.
     *
     * @param context
     */
    void configureContext(MetricsContext context);

    /** Create the appropriate sink for this environment. */
    ISink getSink();
}
