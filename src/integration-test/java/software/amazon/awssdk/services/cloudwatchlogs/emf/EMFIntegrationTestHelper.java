package software.amazon.awssdk.services.cloudwatchlogs.emf;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;

class EMFIntegrationTestHelper {


    boolean checkMetricExistence(GetMetricStatisticsRequest request, double expectedSampleCount) {
        CloudWatchClient client = CloudWatchClient.builder().build();
        GetMetricStatisticsResponse response = client.getMetricStatistics(request);
        if (response == null)
            return false;
        double sampleCounts = response.datapoints().stream()
                .map(Datapoint::sampleCount)
                .reduce(0.0, Double::sum);

        return sampleCounts == expectedSampleCount;
    }
}
