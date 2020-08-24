package agent;

import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;

public class App {

    public static void main(String[] args) {
        MetricsLogger logger = new MetricsLogger();
        logger.putDimensions(DimensionSet.of("Operation", "Agent"));
        logger.putMetric("ExampleMetric", 100, Unit.MILLISECONDS);
        logger.putProperty("RequestId", "422b1569-16f6-4a03-b8f0-fe3fd9b100f8");
        logger.flush();
    }
}
