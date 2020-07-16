package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

import java.util.List;


@Slf4j
public class AgentSink implements ISink {
    private final String logGroupName;
    private final String logStreamName;
    private final SocketClient client;

    public AgentSink(String logGroupName, String logStreamName, Endpoint endpoint, SocketClientFactory clientFactory) {
            this.logGroupName = logGroupName;
            this.logStreamName = logStreamName;
            client = clientFactory.getClient(endpoint);
    }

    @Override
    public void accept(List<EMFLogItem> logItems) throws FlushException {
        //TODO Remove this method
    }

    public void accept(MetricsContext context) {
        if (logGroupName != null && !logGroupName.isEmpty()) {
            context.putProperty("LogGroupName", logGroupName);
        }

        if (logStreamName!= null && !logStreamName.isEmpty()) {
            context.putProperty("LogStreamName", logStreamName);
        }

        client.connect();
        try {
            client.sendMessage(context.serialize());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize the metrics with the exception: ", e);
        }
    }
}
