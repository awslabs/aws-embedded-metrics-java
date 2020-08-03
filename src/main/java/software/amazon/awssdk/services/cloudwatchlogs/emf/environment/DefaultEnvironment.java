package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.Constants;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

@Slf4j
class DefaultEnvironment extends AgentBasedEnvironment {
    private Configuration config;

    DefaultEnvironment(Configuration config) {
        super(config);
        this.config = config;
    }

    @Override
    public boolean probe() {
        return true;
    }

    @Override
    public String getType() {
        if (!config.getServiceType().isPresent()) {
            log.info("Unknown ServiceType");
            return Constants.UNKNOWN;
        }
        return config.getServiceType().get();
    }

    @Override
    public void configureContext(MetricsContext context) {
        // no-op
    }
}
