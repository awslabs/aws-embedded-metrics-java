package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

/** An sink connecting to CloudWatch Agent. */
@Slf4j
public class AgentSink implements ISink {
    private final String logGroupName;
    private final String logStreamName;
    private final SocketClient client;

    public AgentSink(
            String logGroupName,
            String logStreamName,
            Endpoint endpoint,
            SocketClientFactory clientFactory) {
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;
        client = clientFactory.getClient(endpoint);
    }

    public void accept(MetricsContext context) {
        if (logGroupName != null && !logGroupName.isEmpty()) {
            context.putMetadata("LogGroupName", logGroupName);
        }

        if (logStreamName != null && !logStreamName.isEmpty()) {
            context.putMetadata("LogStreamName", logStreamName);
        }

        try {
            client.sendMessage(context.serialize() + "\n");
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize the metrics with the exception: ", e);
        }
    }
}
