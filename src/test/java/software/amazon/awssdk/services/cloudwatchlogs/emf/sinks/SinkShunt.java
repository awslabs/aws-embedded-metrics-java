package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

public class SinkShunt implements ISink {

    private MetricsContext context;

    @Override
    public void accept(MetricsContext context) {
        this.context = context;
    }

    public MetricsContext getContext() {
        return context;
    }
}
