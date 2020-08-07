import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.Unit;

import java.util.HashMap;
import java.util.Map;

public class Handler implements RequestHandler<Map<String,String>, String> {

    @Override
    public String handleRequest(Map<String,String> event, Context context) {
        String response = "200 OK";
        MetricsLogger logger = new MetricsLogger();

        logger.putDimensions(DimensionSet.of("Service", "Aggregator"));
        logger.putMetric("ProcessingLatency", 100, Unit.MILLISECONDS);
        logger.putProperty("AccountId", "123456789");
        logger.putProperty("RequestId", "422b1569-16f6-4a03-b8f0-fe3fd9b100f8");
        logger.putProperty("DeviceId", "61270781-c6ac-46f1-baf7-22c808af8162");
        Map<String, Object> payLoad = new HashMap<>();
        payLoad.put("sampleTime", 123456789);
        payLoad.put("temperature", 273.0);
        payLoad.put("pressure", 101.3);
        logger.putProperty("Payload", payLoad);
        logger.flush();

        System.out.println("completed aggregation successfully.");

        return response;
    }

}
