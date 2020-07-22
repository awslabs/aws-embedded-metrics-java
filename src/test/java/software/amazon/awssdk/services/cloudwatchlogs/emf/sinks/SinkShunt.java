package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

import java.util.List;

public class SinkShunt implements ISink {

    private MetricsContext context;

    @Override
    public void accept(List<EMFLogItem> logItems) throws FlushException {

    }

    @Override
    public void accept(MetricsContext context) {
        this.context = context;
    }

    public MetricsContext getContext() {
        return context;
    }

}
