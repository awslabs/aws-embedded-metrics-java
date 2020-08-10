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

import software.amazon.cloudwatchlogs.emf.environment.Environments;
import software.amazon.cloudwatchlogs.emf.util.StringUtils;

/** Loads configuration from environment variables. */
public class EnvironmentConfigurationProvider {
    private static Configuration config;

    protected EnvironmentConfigurationProvider() {}

    public static Configuration getConfig() {
        if (config == null) {
            config =
                    new Configuration(
                            getEnvVar(ConfigurationKeys.SERVICE_NAME),
                            getEnvVar(ConfigurationKeys.SERVICE_TYPE),
                            getEnvVar(ConfigurationKeys.LOG_GROUP_NAME),
                            getEnvVar(ConfigurationKeys.LOG_STREAM_NAME),
                            getEnvVar(ConfigurationKeys.AGENT_ENDPOINT),
                            getEnvironmentOverride());
        }
        return config;
    }

    private static String getEnvVar(String key) {
        String name = String.join("", ConfigurationKeys.ENV_VAR_PREFIX, "_", key);
        return getEnv(name);
    }

    private static Environments getEnvironmentOverride() {
        String environmentName = getEnvVar(ConfigurationKeys.ENVIRONMENT_OVERRIDE);
        if (StringUtils.isNullOrEmpty(environmentName)) {
            return Environments.Unknown;
        }

        try {
            return Environments.valueOf(environmentName);
        } catch (Exception e) {
            return Environments.Unknown;
        }
    }

    private static String getEnv(String name) {
        return SystemWrapper.getenv(name);
    }
}
