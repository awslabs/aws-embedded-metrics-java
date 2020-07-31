package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

/** Interface for sinking log items to CloudWatch. */
public interface ISink {

    /** accept MetricsContext to sink to CloudWatch. */
    void accept(MetricsContext context);
}
