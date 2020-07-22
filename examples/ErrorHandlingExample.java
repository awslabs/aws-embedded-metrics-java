import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.EMFLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.CloudWatchLogsClientSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.MultiSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.CloudwatchMetricCollection;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.time.Instant;

public class ErrorHandlingExample {

    public static void main(String[] args) throws FlushException {
        final String logGroup = "exampleLogGroup";
        final String logStream = "exampleLogStream";
        String namespace = "exampleNamespace4";

        CloudWatchLogsClient logsClient = CloudWatchLogsClient
                .builder()
                .region(Region.of("us-west-1"))
                .build();


        simpleExample(logsClient, logGroup, logStream, namespace);
    }


    protected static EMFLogger createEMFLogger(
            CloudWatchLogsClient logsClient,
            String logGroup,
            String logStream) {

        EMFLogger logger = EMFLogger.builder()
                .logSink(MultiSink.builder()
                        .sink(ConsoleSink.builder().build())
                        .sink(
                                CloudWatchLogsClientSink.builder()
                                        .client(logsClient)
                                        .logGroup(logGroup)
                                        .logStream(logStream)
                                        .build()
                        )
                        .build()
                ).build();

        return logger;
    }

    public static void simpleExample(
            CloudWatchLogsClient logsClient,
            String logGroup,
            String logStream,
            String namespace) {

        EMFLogger logger = createEMFLogger(logsClient, logGroup, logStream);

        EMFLogItem emf1 = logger.createLogItem();
        // Metrics can contain raw log messages to will follow the metrics in the log entry
        // This allows you to output metrics, then lookup log entries on the metric values, and correlate more
        // information that metrics alone can convey.
        emf1.setRawLogMessage("Hello world!");
        CloudwatchMetricCollection mc1 = emf1.createMetricsCollection();
        mc1.setNamespace(namespace);
        mc1.putMetric("aMetric1", 1, StandardUnit.NONE);
        mc1.putProperty("aProperty1", 1);
        mc1.putDimension("aDimension1", "aDimensionValue1");


        EMFLogItem emf2 = logger.createLogItem();
        // Create a timestamp out of order with the above.
        emf2.setTimestamp(Instant.now().minusSeconds(60));
        emf2.setRawLogMessage("Hello world!");
        CloudwatchMetricCollection mc2 = emf1.createMetricsCollection();
        mc2.setNamespace(namespace);
        mc2.putMetric("aMetric1", 1, StandardUnit.NONE);
        mc2.putProperty("aProperty1", 1);
        mc2.putDimension("aDimension1", "aDimensionValue1");

        try {
            logger.flush();
        } catch (FlushException e) {
            // What do we do here?  How do we handle this error?

            e.printStackTrace();
        }
    }

}
