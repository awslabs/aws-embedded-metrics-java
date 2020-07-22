package software.amazon.awssdk.services.cloudwatchlogs.emf.testutils;

import lombok.Getter;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Test sink class for doing a flush.
 *
 * Just saves all of the log items it sees.
 *
 */
public class TestSink implements ISink {
    @Getter
    private List<EMFLogItem> seenLogItems = new ArrayList<>();

    @Override
    public void accept(List<EMFLogItem> logItems) throws FlushException {
        seenLogItems.addAll(logItems);
    }

    @Override
    public void accept(MetricsContext context) {
        // no-op
    }
}
