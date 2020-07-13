import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.EMFLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks.CloudWatchLogsClientSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks.ConsoleSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.sinks.MultiSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.Aggregation;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.CloudwatchMetricCollection;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

public class Examples {

    public static void main(String[] args) throws FlushException {
        final String logGroup = "exampleLogGroup";
        final String logStream = "exampleLogStream";
        String namespace = "exampleNamespace4";

        CloudWatchLogsClient logsClient = CloudWatchLogsClient
                .builder()
                .region(Region.of("us-west-1"))
                .build();

        simpleExample(logsClient, logGroup, logStream, namespace);
        multipleDimensionAggregations(logsClient, logGroup, logStream, namespace);
        complexPropertyExample(logsClient, logGroup, logStream, namespace);
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
            String namespace) throws FlushException {

        EMFLogger logger = createEMFLogger(logsClient, logGroup, logStream);

        EMFLogItem emf = logger.createLogItem();
        // Metrics can contain raw log messages to will follow the metrics in the log entry
        // This allows you to output metrics, then lookup log entries on the metric values, and correlate more
        // information that metrics alone can convey.
        emf.setRawLogMessage("Hello world!");
        CloudwatchMetricCollection mc1 = emf.createMetricsCollection();
        mc1.setNamespace(namespace);
        mc1.putMetric("aMetric1", 1, StandardUnit.NONE);
        mc1.putProperty("aProperty1", 1);
        mc1.putDimension("aDimension1", "aDimensionValue1");

        logger.flush();
    }




    // Send metrics with multiple dimension aggregations
    public static void multipleDimensionAggregations(
            CloudWatchLogsClient logsClient,
            String logGroup,
            String logStream,
            String namespace) throws FlushException {

        EMFLogger logger = createEMFLogger(logsClient, logGroup, logStream);

        EMFLogItem emf1 = logger.createLogItem();
        emf1.setRawLogMessage("Hello world!");
        CloudwatchMetricCollection mc1 = emf1.createMetricsCollection();
        mc1.setNamespace(namespace);

        mc1.putMetric("aMetric1", 1, StandardUnit.NONE);

        // Create a metric that will be overwritten by the following metric collection
        mc1.putMetric("aSharedMetric1", 2);

        mc1.putProperty("aProperty1", 1);
        // putDimension will create a default aggregation
        // This can be mixed with adding additional aggregations as well
        mc1.putDimension("aDimension1", "aDimensionValue1");

        // Create a shared dimension that will be overwritten in the following metric collection
        mc1.putDimension("aSharedDimension1", "aSharedDimensionValue1");

        // When using putDimensionAggregation you must also explicitly set value for any new dimensions
        Aggregation agg1 = mc1.putDimensionAggregation("aDimension1", "aDimension2");
        // aDimension1 already had its value set in the putDimension call above
        // so only aDimension2 needs to have a value set
        agg1.setDimensionValue("aDimension2", "aDimensionValue2");

        // Additional aggregations don't need to share dimensions
        Aggregation agg2 = mc1.putDimensionAggregation("aDimension3", "aDimension4");
        agg2.setDimensionValue("aDimension3", "aDimensionValue3");
        agg2.setDimensionValue("aDimension4", "aDimensionValue4");



        // dimension values, metric values, and property values are shared across multiple
        // metric collections.
        CloudwatchMetricCollection mc2 = emf1.createMetricsCollection();
        mc2.setNamespace(namespace);
        mc2.putMetric("aMetric2", 2, StandardUnit.NONE);

        // Note this call changes the value for aSharedMetric1 set earlier
        mc2.putMetric("aSharedMetric1", 4);

        // Note this changes the value for the dimension set earlier.
        mc2.putDimension("aSharedDimension1", "aSharedDimensionValueChanged");



        logger.flush();
    }




    // Create a log entry using EMF with a complex property.
    @AllArgsConstructor
    static class ComplexProperty
    {
        @Getter
        @Setter
        private String stringVal;

        @Getter @Setter
        private int intVal;
    }

    public static void complexPropertyExample(
            CloudWatchLogsClient logsClient,
            String logGroup,
            String logStream,
            String namespace) throws FlushException {

        EMFLogger logger = createEMFLogger(logsClient, logGroup, logStream);

        EMFLogItem emf = logger.createLogItem();
        emf.setRawLogMessage("Hello world!");
        CloudwatchMetricCollection mc1 = emf.createMetricsCollection();
        mc1.setNamespace(namespace);
        mc1.putMetric("myNewMetricName2", 1, StandardUnit.NONE);
        mc1.putProperty("aProperty", 1);
        mc1.putDimension("aDimension", "aDimensionValue");

        ComplexProperty complexProperty = new ComplexProperty("stringValue1", 2);
        mc1.putProperty("aComplexProperty", complexProperty);

        logger.flush();
    }
}
