package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

import java.util.Arrays;
import java.util.List;

/**
 * Write log items to the console in JSON format.
 */
@Slf4j
@Builder
@NoArgsConstructor
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

    @Override
    public void accept(MetricsContext context) {

        try {
            System.out.println(context.serialize());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize a MetricsContext: ", e);
        }
    }

}
