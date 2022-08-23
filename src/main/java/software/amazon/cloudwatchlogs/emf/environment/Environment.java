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

import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.ISink;

/**
 * A runtime environment (e.g. Lambda, EKS, ECS, EC2).
 */
public interface Environment {

    /**
     * Determines whether or not we are executing in this environment.
     *
     * @return true if it is running in this environment, otherwise, false
     */
    boolean probe();

    /**
     * Get the environment name. This will be used to set the ServiceName dimension.
     *
     * @return the name of the environment
     */
    String getName();

    /**
     * Get the environment type. This will be used to set the ServiceType dimension.
     *
     * @return the type of the environment
     */
    String getType();

    /**
     * Get log group name. This will be used to set the LogGroup dimension.
     *
     * @return the log group name
     */
    String getLogGroupName();

    /**
     * @param context the context to configure with environment properties
     */
    void configureContext(MetricsContext context);

    /**
     * @return an appropriate sink for this environment
     */
    ISink getSink();
}
