package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;


/**
 * Write log items to the console in JSON format.
 */
@Slf4j
@Builder
@NoArgsConstructor
public class ConsoleSink implements ISink {


    @Override
    public void accept(MetricsContext context) {

        try {
            System.out.println(context.serialize());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize a MetricsContext: ", e);
        }
    }

}
