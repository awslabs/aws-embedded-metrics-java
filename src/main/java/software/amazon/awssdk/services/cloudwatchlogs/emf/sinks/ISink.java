package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

import java.util.List;

/**
 * Interface for sinking log items to CloudWatch.
 */
public interface ISink {
    /**
     * accept logItems to sink to CloudWatch.
     * @param logItems list of logItems to sink.
     * @throws FlushException
     */
    void accept(List<EMFLogItem> logItems) throws FlushException;

    /**
     * accept MetricsContext to sink to CloudWatch.
     */
    void accept(MetricsContext context);
}
