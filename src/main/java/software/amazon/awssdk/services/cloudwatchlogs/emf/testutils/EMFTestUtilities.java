package software.amazon.awssdk.services.cloudwatchlogs.emf.testutils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.Aggregation;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.CloudwatchMetricCollection;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;

import java.util.Random;

/**
 * Test utilities for EMF unit tests.
 */
public final class EMFTestUtilities {
    @Getter
    private static Random rand;

    static {
        long seed = System.currentTimeMillis();
        rand = new Random(seed);
        System.out.printf("Random seed for tests: %d%n", seed);
    }

    protected EMFTestUtilities(){}

    public static int randInt(int min, int max) {
        int randomNum = getRand().nextInt((max - min) + 1) + min;
        return randomNum;
    }


    public static EMFLogItem createTinyLogItem(EMFLogItem logItem, int id) {
        CloudwatchMetricCollection metricsCollection = logItem.createMetricsCollection();
        metricsCollection.putDimension("d", "1");
        metricsCollection.putMetric("m", 2);
        metricsCollection.setNamespace("a");
        return logItem;
    }


    public static EMFLogItem createTinyLogItem(int id) {
        EMFLogItem logItem = new EMFLogItem();
        createTinyLogItem(logItem, id);
        return logItem;
    }


    public static EMFLogItem createLargeLogItem(EMFLogItem logItem, int id) {
        CloudwatchMetricCollection metricsCollection = logItem.createMetricsCollection();
        metricsCollection.putDimension("dim1", "dimVal1");
        metricsCollection.putDimension("dim2", "dimVal2");
        metricsCollection.putDimensionAggregation("dim1");
        metricsCollection.putDimensionAggregation("dim2");
        Aggregation agg = metricsCollection.putDimensionAggregation("dim2", "dim1", "dim4");
        agg.addDimension("dim3");
        agg.setDimensionValue("dim3", "dimVal3");
        agg.addDimension("dim4", "dimVal4");


        metricsCollection.putMetric("metric1", 1);
        metricsCollection.putMetric("metric2", 2);
        metricsCollection.putProperty("stringProperty1", "simpleStringProp1");
        metricsCollection.putProperty("intProperty1", 3);
        metricsCollection.setNamespace("aNamespace");
        logItem.setRawLogMessage(String.format("%d", id));
        return logItem;
    }


    public static EMFLogItem createLargeLogItem(int id) {
        EMFLogItem logItem = new EMFLogItem();
        createLargeLogItem(logItem, id);
        return logItem;
    }



    @AllArgsConstructor
    static class ComplexProperty {
        @Getter
        private String stringVal;

        @Getter
        private int intVal;
    }

    public static EMFLogItem createComplexLogItem(int id) {
        // Create a log item to serialize
        EMFLogItem logItem = new EMFLogItem();
        CloudwatchMetricCollection metricsCollection = logItem.createMetricsCollection();
        metricsCollection.putDimension("dim1", "dimVal1");
        metricsCollection.putDimension("dim2", "dimVal2");
        metricsCollection.putDimensionAggregation("dim1");
        metricsCollection.putDimensionAggregation("dim2");
        metricsCollection.putMetric("metric1", 1);
        metricsCollection.putMetric("metric2", 2);
        metricsCollection.putProperty("stringProperty1", "simpleStringProp1");
        metricsCollection.putProperty("intProperty1", 3);
        metricsCollection.putProperty("complexProperty1", new ComplexProperty("aString", 4));
        metricsCollection.setNamespace("aNamespace");
        logItem.setRawLogMessage(String.format("%d", id));
        return logItem;
    }


    /**
     * Test interface for creating simple lambdas that throw exceptions.
     */
    public interface ExceptionalCall {
        void f() throws Throwable;
    }

    public static <T> boolean checkThrows(ExceptionalCall f, Class<T> exceptionType) {
        boolean threwRightException = false;
        try {
            f.f();
        } catch (Throwable t) {
            threwRightException = t.getClass().equals(exceptionType);
        }
        return threwRightException;
    }
}
