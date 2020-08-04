package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.Constants;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.AgentSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.Endpoint;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.SocketClientFactory;

@Slf4j
public abstract class AgentBasedEnvironment implements Environment {
    private Configuration config;
    private ISink sink;

    public AgentBasedEnvironment(Configuration config) {
        this.config = config;
    }

    @Override
    public String getName() {
        if (!config.getServiceName().isPresent()) {
            log.warn("Unknown ServiceName.");
            return Constants.UNKNOWN;
        }
        return config.getServiceName().get();
    }

    @Override
    public String getLogGroupName() {
        return config.getLogGroupName().orElse(getName() + "-metrics");
    }

    public String getLogStreamName() {
        return config.getLogStreamName().orElse(getName() + "-stream");
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
