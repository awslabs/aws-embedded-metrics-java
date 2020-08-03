package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import java.util.Optional;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;

/** A provider that will detect the environment. */
public class EnvironmentProvider {
    private static Environment cachedEnvironment;
    private final Configuration config = EnvironmentConfigurationProvider.getConfig();
    private final Environment lambdaEnvironment = new LambdaEnvironment();
    private final Environment defaultEnvironment = new DefaultEnvironment(config);
    private final Environment ec2Environment = new EC2Environment(config, new ResourceFetcher());
    private final Environment ecsEnvironment = new ECSEnvironment(config, new ResourceFetcher());

    // Ordering of this array matters
    private final Environment[] environments =
            new Environment[] {
                lambdaEnvironment, ec2Environment, ecsEnvironment, defaultEnvironment
            };

    // TODO: Support more environments
    public Environment resolveEnvironment() {
        if (cachedEnvironment != null) {
            return cachedEnvironment;
        }

        Optional<Environment> env = getEnvironmentFromOverride();

        cachedEnvironment = env.orElseGet(() -> discoverEnvironment().orElse(defaultEnvironment));
        return cachedEnvironment;
    }

    /** A helper method to clean the cached environment in tests. */
    void cleanResolvedEnvironment() {
        cachedEnvironment = null;
    }

    private Optional<Environment> discoverEnvironment() {
        for (Environment env : environments) {
            if (env.probe()) {
                return Optional.of(env);
            }
        }
        return Optional.empty();
    }

    private Optional<Environment> getEnvironmentFromOverride() {
        Configuration config = EnvironmentConfigurationProvider.getConfig();

        Optional<Environment> environment;
        switch (config.getEnvironmentOverride()) {
            case Lambda:
                environment = Optional.of(lambdaEnvironment);
                break;
            case Agent:
                environment = Optional.of(defaultEnvironment);
                break;
            case EC2:
                environment = Optional.of(ec2Environment);
                break;
            case ECS:
                environment = Optional.of(ecsEnvironment);
                break;
            default:
                environment = Optional.empty();
        }
        return environment;
    }
}
