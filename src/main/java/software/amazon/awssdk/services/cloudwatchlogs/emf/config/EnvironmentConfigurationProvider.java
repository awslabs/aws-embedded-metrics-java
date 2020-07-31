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
