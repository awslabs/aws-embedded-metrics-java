package software.amazon.awssdk.services.cloudwatchlogs.emf.logger;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Logger to log metrics in embedded metrics format.
 */
public class EMFLogger {
    @Getter(AccessLevel.PROTECTED)
    private List<EMFLogItem> logItems = new ArrayList<>();

    private final ISink logSink;

    @Builder
    protected EMFLogger(@NonNull ISink logSink) {
        this.logSink = logSink;
    }

    /**
     * Turn JSON pretty printing on or off.
     *
     * This can be useful for debugging the output.
     * Disabled by default.
     * @param enable
     */
    public void enableJsonPrettyPrinting(boolean enable) {
        EMFLogItem.setGlobalPrettyPrintJson(enable);
    }

    /**
     * Create a new log item.
     * @return a new log item.
     */
    public EMFLogItem createLogItem() {
        EMFLogItem logItem = new EMFLogItem();
        logItems.add(logItem);
        return logItem;
    }

    /**
     * Append existing log items.
     * @param logItems
     */
    public void addLogItems(List<EMFLogItem> logItems) {
        for (EMFLogItem li: logItems) {
            getLogItems().add(li);
        }
    }

    /**
     * Flush all existing log items to the given sink.
     * @throws FlushException
     */
    public void flush() throws FlushException {
        try {
            logSink.accept(logItems);
        } finally {
            logItems.clear();
        }
    }
}
