package software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.Arrays;
import java.util.List;

/**
 * Write log items to the console in JSON format.
 */
@Builder
public class ConsoleSink implements ISink {

    @Override
    public void accept(List<EMFLogItem> logItems) throws FlushException {
        for (int i = 0; i < logItems.size(); ++i) {
            try {
                String json = logItems.get(i).serialize();
                System.out.println(json);
            } catch (JsonProcessingException e) {
                List<EMFLogItem> failedLogItems = Arrays.asList(logItems.get(i));
                throw new FlushException("Failed to serialize an EMFLogItem", e, failedLogItems, null);
            }
        }
    }
}
