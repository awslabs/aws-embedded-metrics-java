package software.amazon.awssdk.services.cloudwatchlogs.emf;

import com.fasterxml.jackson.core.JsonProcessingException;
import software.amazon.awscdk.services.cloudwatch.Unit;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricDataResponse;
import software.amazon.awssdk.services.cloudwatch.model.Metric;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataQuery;
import software.amazon.awssdk.services.cloudwatch.model.MetricDataResult;
import software.amazon.awssdk.services.cloudwatch.model.MetricStat;
import software.amazon.awssdk.services.cloudwatch.model.ScanBy;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.EMFLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.FlushException;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.Aggregation;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.CloudwatchMetricCollection;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.GetLogEventsResponse;
import software.amazon.awssdk.services.cloudwatchlogs.model.OutputLogEvent;

import java.time.Instant;
import java.util.stream.Collectors;

public class MetricsIngestedFromEMFTest extends IntegrationTestBase {
    public static void main(String[] args) throws FlushException, JsonProcessingException, InterruptedException {
        CloudWatchLogsClient logsClient = createCloudWatchLogsClient();
        EMFLogger logger = createEMFLogger(logsClient);

        testMetricsIngestion(logger, logsClient);

        System.out.printf("Done!\n");

        System.exit(0);
    }


    public static void testMetricsIngestion(EMFLogger logger, CloudWatchLogsClient logsClient) throws FlushException, InterruptedException {
        int errorCount = 0;
        Instant startTime = Instant.now().minusSeconds(60);

        String testMetricName =  getRandomString(6);
        int testMetricValue = EMFTestUtilities.randInt(0, 10000);
        StandardUnit testMetricUnit = StandardUnit.values()[EMFTestUtilities.randInt(0, Unit.values().length-1)];

        String testDimensionName1 = getRandomString(6);
        String testDimensionValue1 =  getRandomString(6);

        String testDimensionName2 =  getRandomString(6);
        String testDimensionValue2 =  getRandomString(6);

        String testPropertyName =  getRandomString(6);
        String testPropertyValue = getRandomString(32);
        String testLogMessage = getRandomString(50);

        // Create the log item
        EMFLogItem logItem = logger.createLogItem();
        logItem.setRawLogMessage(testLogMessage);

        CloudwatchMetricCollection metricsCollection = logItem.createMetricsCollection();
        metricsCollection.setNamespace(namespace);

        metricsCollection.putMetric(testMetricName, testMetricValue, testMetricUnit);
        metricsCollection.putDimension(testDimensionName1, testDimensionValue1);

        Aggregation aggregation2 = metricsCollection.putDimensionAggregation(testDimensionName2);
        aggregation2.setDimensionValue(testDimensionName2, testDimensionValue2);

        metricsCollection.putDimensionAggregation(testDimensionName2, testDimensionName1);

        metricsCollection.putProperty(testPropertyName, testPropertyValue);

        logger.flush();

        Thread.sleep(10*1000);

        GetLogEventsRequest getLogEventsRequest = GetLogEventsRequest.builder()
                .logGroupName(logGroup)
                .logStreamName(logStream)
                .startTime(startTime.toEpochMilli())
                .limit(100)
                .build();

        int foundLogMessage = 0;
        GetLogEventsResponse logEventsResp = logsClient.getLogEvents(getLogEventsRequest);
        for (OutputLogEvent ole : logEventsResp.events()) {
            if (ole.message().contains(testLogMessage))
                foundLogMessage++; // success
        }

        if (foundLogMessage != 1) {
            ++errorCount;
            System.out.printf("testMetricsIngestion: Expected to find log entry once, instead found it %dx%n", foundLogMessage);
        }



        Instant endTime = Instant.now().plusSeconds(60);
        // Now see if the data exists
        CloudWatchClient cwClient = createCloudWatchClient();
        GetMetricDataRequest getMetricDataRequest = GetMetricDataRequest.builder()
                .startTime(startTime)
                .endTime(endTime)
                .maxDatapoints(1000)
                .scanBy(ScanBy.TIMESTAMP_DESCENDING)
                .metricDataQueries(MetricDataQuery.builder()
                        .id("myQueryId")
                        .metricStat(MetricStat.builder()
                                .period(1)
                                .stat("p100")
                                .unit(testMetricUnit.toString())
                                .metric(Metric.builder()
                                        .metricName(testMetricName)
                                        .namespace(namespace)
                                        .dimensions(Dimension.builder()
                                                .name(testDimensionName1)
                                                .value(testDimensionValue1)
                                                .build())
                                        .build())
                                .build())
                        .build())
                .build();

        GetMetricDataResponse metricDataResp = cwClient.getMetricData(getMetricDataRequest);
        boolean foundMetricData = false;
        for (MetricDataResult mr : metricDataResp.metricDataResults()) {
            if (mr.values().stream().filter(i -> i == testMetricValue).collect(Collectors.toList()).size() == 1) {
                foundMetricData = true;
            }
        }

        if (!foundMetricData) {
            ++errorCount;
            System.out.printf("testMetricsIngestion: Couldn't find metric data%n");
        }

        if (errorCount > 0) {
            System.exit(-1);
        }
    }
}
