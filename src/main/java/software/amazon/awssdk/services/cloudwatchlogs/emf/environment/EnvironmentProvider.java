package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import software.amazon.awssdk.services.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;

public class EnvironmentProvider {

    //TODO: Support more environments
    public Environment resolveEnvironment() {
        return new DefaultEnvironment(EnvironmentConfigurationProvider.getConfig());
    }
}
