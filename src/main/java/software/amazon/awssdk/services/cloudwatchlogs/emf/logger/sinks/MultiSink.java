package software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.List;

/**
 * Convenience helper for sinking the same log items to multiple destinations.
 *
 * Useful for debugging, sink to CloudWatch and console.
 */
@Builder
public class MultiSink implements ISink {
    @Singular
    @NonNull
    private List<ISink> sinks;

    @Override
    public void accept(List<EMFLogItem> logItems) throws FlushException {
        for (ISink sink : sinks) {
            sink.accept(logItems);
        }
    }
}
