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
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.Environments;

/** Loads configuration from environment variables. */
public class EnvironmentConfigurationProvider {
    private static Configuration config;

    protected EnvironmentConfigurationProvider() {}

    public static Configuration getConfig() {
        if (config == null) {
            config =
                    new Configuration(
                            getBoolEnvVar(ConfigurationKeys.ENABLE_DEBUG_LOGGING),
                            getEnvVar(ConfigurationKeys.SERVICE_NAME),
                            getEnvVar(ConfigurationKeys.SERVICE_TYPE),
                            getEnvVar(ConfigurationKeys.LOG_GROUP_NAME),
                            getEnvVar(ConfigurationKeys.LOG_STREAM_NAME),
                            getEnvVar(ConfigurationKeys.AGENT_ENDPOINT),
                            getEnvironmentOverride());
        }
        return config;
    }

    private static Optional<String> getEnvVar(String key) {
        String name = String.join("", ConfigurationKeys.ENV_VAR_PREFIX, "_", key);
        return Optional.ofNullable(getEnv(name));
    }

    private static boolean getBoolEnvVar(String key) {
        String name = String.join("", ConfigurationKeys.ENV_VAR_PREFIX, "_", key);
        return Optional.ofNullable(getEnv(name))
                .map(str -> str.equalsIgnoreCase("true"))
                .orElse(false);
    }

    private static Environments getEnvironmentOverride() {
        Optional<String> environmentName = getEnvVar(ConfigurationKeys.ENVIRONMENT_OVERRIDE);
        if (!environmentName.isPresent()) {
            return Environments.Unknown;
        }

        try {
            return Environments.valueOf(environmentName.get());
        } catch (Exception e) {
            return Environments.Unknown;
        }
    }

    private static String getEnv(String name) {
        return SystemWrapper.getenv(name);
    }
}
