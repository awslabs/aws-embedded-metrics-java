package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.Constants;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;

@Slf4j
class LocalEnvironment implements Environment {
    private ISink sink;
    private Configuration config;

    LocalEnvironment(Configuration config) {
        this.config = config;
    }

    // probe is not intended to be used in the LocalEnvironment
    // To use the local environment you should set the environment
    // override
    @Override
    public boolean probe() {
        return false;
    }

    @Override
    public String getName() {
        if (config.getServiceName().isPresent()) {
            return config.getServiceName().get();
        }
        log.info("Unknown name");
        return Constants.UNKNOWN;
    }

    @Override
    public String getType() {
        if (config.getServiceType().isPresent()) {
            return config.getServiceType().get();
        }
        log.info("Unknown type");
        return Constants.UNKNOWN;
    }

    @Override
    public String getLogGroupName() {
        return config.getLogGroupName().orElse(getName() + "-metrics");
    }

    @Override
    public void configureContext(MetricsContext context) {
        // no-op
    }

    @Override
    public ISink getSink() {
        if (sink == null) {
            this.sink = new ConsoleSink();
        }
        return this.sink;
    }
}
