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

/** The key of configurations used by EMF logger. */
public class ConfigurationKeys {

    public static final String ENV_VAR_PREFIX = "AWS_EMF";

    public static final String SERVICE_NAME = "SERVICE_NAME";
    public static final String SERVICE_TYPE = "SERVICE_TYPE";
    public static final String LOG_GROUP_NAME = "LOG_GROUP_NAME";
    public static final String LOG_STREAM_NAME = "LOG_STREAM_NAME";
    public static final String AGENT_ENDPOINT = "AGENT_ENDPOINT";
    public static final String ENVIRONMENT_OVERRIDE = "ENVIRONMENT";
}
