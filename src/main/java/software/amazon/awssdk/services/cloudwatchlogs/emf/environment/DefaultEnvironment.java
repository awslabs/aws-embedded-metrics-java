package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.AgentSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.Endpoint;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.SocketClientFactory;

@Slf4j
public class DefaultEnvironment implements Environment {
    private Configuration config;
    private ISink sink;

    public DefaultEnvironment(Configuration config) {
        this.config = config;
    }

    @Override
    public boolean probe() {
        return true;
    }

    @Override
    public String getName() {
        if (!config.getServiceName().isPresent()) {
            log.info("Unknown ServiceName");
            return "Unknown";
        }
        return config.getServiceName().get();
    }

    @Override
    public String getType() {
        if (!config.getServiceType().isPresent()) {
            log.info("Unknown ServiceType");
            return "Unknown";
        }
        return config.getServiceType().get();
    }

    public String getLogStreamName() {
        return config.getLogStreamName().orElse(getName() + "-stream");
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
            Endpoint endpoint;
            if (!config.getAgentEndpoint().isPresent()) {
                log.info(
                        "Endpoint is not defined. Using default: {}",
                        Endpoint.DEFAULT_TCP_ENDPOINT);
                endpoint = Endpoint.DEFAULT_TCP_ENDPOINT;
            } else {
                endpoint = Endpoint.fromURL(config.getAgentEndpoint().get());
            }
            sink =
                    new AgentSink(
                            getLogGroupName(),
                            getLogStreamName(),
                            endpoint,
                            new SocketClientFactory());
        }
        return sink;
    }
}
