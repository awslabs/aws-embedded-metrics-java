package software.amazon.awssdk.services.cloudwatchlogs.emf;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.EMFLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.CloudWatchLogsClientSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.testutils.EMFTestUtilities;

public class IntegrationTestBase {
    final static String logGroup = "exampleLogGroup";
    final static String logStream = "exampleLogStream";

    final static String dimensionName = "aDimension1";
    final static String dimensionValue = "aDimensionValue1";
    final static String namespace = "exampleNamespace4";

    final static String metricName = "aMetricName1";

    final static String region = "us-west-1";

    static CloudWatchClient createCloudWatchClient() {
        CloudWatchClient cwClient = CloudWatchClient.builder()
                .region(Region.of(region))
                .build();

        return cwClient;
    }

    static CloudWatchLogsClient createCloudWatchLogsClient() {
        CloudWatchLogsClient logsClient = CloudWatchLogsClient
                .builder()
                .region(Region.of(region))
                .build();

        return logsClient;
    }


    static EMFLogger createEMFLogger(CloudWatchLogsClient logsClient) {
        CloudWatchLogsClientSink cloudWatchLogsClientSink = CloudWatchLogsClientSink.builder()
                .client(logsClient)
                .logGroup(logGroup)
                .logStream(logStream)
                .build();

        EMFLogger logger = EMFLogger.builder()
                .logSink(cloudWatchLogsClientSink)
                .build();

        return logger;
    }


    public static String getRandomString(int length) {
        byte[] possibleBytes = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p',
                'q','r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
        String possibleChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; ++i) {
            int idx = EMFTestUtilities.randInt(0, possibleChars.length()-1);
            sb.append(possibleChars.charAt(idx));
        }
        return sb.toString();
    }

}
