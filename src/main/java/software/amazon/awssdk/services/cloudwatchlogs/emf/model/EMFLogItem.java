package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Map;

/**
 * EMFLogItem.
 */
public class EMFLogItem {

    private RootNode rootNode = new RootNode();
    @Getter
    @Setter
    private String rawLogMessage;

    /** Turn on JSON pretty printing for all EMFLogItems */
    @Getter
    @Setter
    private static boolean globalPrettyPrintJson = false;

    public EMFLogItem() {
    }

    /**
     * Serialize only the metrics, not the rawLogMessage.
     * @return JSON in EMF format of metrics
     * @throws JsonProcessingException
     */
    public String serializeMetrics() throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();

        if (EMFLogItem.isGlobalPrettyPrintJson()) {
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        }

        String ret = objectMapper.writeValueAsString(rootNode);
        return ret;
    }

    /**
     * Serialize the metrics to JSON, followed by a raw log message exactly as is.
     * @return JSON in EMF format of metrics, followed by the unformatted rawLogMessage string
     * @throws JsonProcessingException
     */
    public String serialize() throws JsonProcessingException {
        StringBuilder sb = new StringBuilder();
        sb.append(serializeMetrics());

        if (rawLogMessage != null) {
            sb.append(System.lineSeparator());
            sb.append(rawLogMessage);
        }

        return sb.toString();
    }

    /**
     * Set the timestamp for these metrics, and the CloudWatch InputLogEvent.
     * @param timestamp
     */
    public void setTimestamp(Instant timestamp) {
        rootNode.getAws().setTimestamp(timestamp);
    }

    public Instant getTimestamp() {
        return rootNode.getAws().getTimestamp();
    }


    /**
     * Create a CloudwatchMetricCollection, and add it to the internal list for later flushing.
     * @return newly created CloudWatchMetricCollection
     */
    public CloudwatchMetricCollection createMetricsCollection() {
        CloudwatchMetricCollection newMetricCollection = new CloudwatchMetricCollection(
                rootNode,
                rootNode.getAws().createMetricDirective()
        );
        return newMetricCollection;
    }

    public Map<String, Object> getProperties() {
        return rootNode.getMetricsAndProperties();
    }
}
