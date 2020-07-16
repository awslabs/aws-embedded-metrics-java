package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import org.junit.Test;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CloudWatchLogsClientSinkNotInOrderTest extends CloudWatchLogsClientSinkTestBase {
    @Test
    public void testLogItemsNotInOrder() {
        CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClientMock();
        CloudWatchLogsClientSink cwSink = CloudWatchLogsClientSink.builder()
                .client(cloudWatchLogsClient)
                .logGroup("aLogGroup")
                .logStream("aLogStream")
                .build();

        List<EMFLogItem> logItems = new ArrayList<>();

        EMFLogItem li1 = new EMFLogItem();
        li1.setTimestamp(Instant.now());
        logItems.add(li1);

        EMFLogItem li2 = new EMFLogItem();
        li2.setTimestamp(Instant.now().minusSeconds(1));
        logItems.add(li2);

        boolean exceptionTaken = false;
        try {
            cwSink.accept(logItems);
        } catch(FlushException e) {
            exceptionTaken = true;
            assertEquals(itemsOutOfOrderMessage, e.getMessage());
            assertEquals(logItems.size(), e.getUnprocessedLogItems().size());
            matchLists("Log items needs to match unprocessed log items", logItems, e.getUnprocessedLogItems());
        }
        assertTrue(exceptionTaken);
    }
}
