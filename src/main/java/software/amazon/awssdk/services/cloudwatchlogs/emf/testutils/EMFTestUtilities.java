/*
 *   Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package software.amazon.awssdk.services.cloudwatchlogs.emf.testutils;

import lombok.AllArgsConstructor;
import lombok.Getter;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.EMFLogItem;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;

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
        MetricsContext metricsContext = logItem.createMetricsContext();
        metricsContext.putDimension("d", "1");
        metricsContext.putMetric("m", 2);
        metricsContext.setNamespace("a");
        return logItem;
    }


    public static EMFLogItem createTinyLogItem(int id) {
        EMFLogItem logItem = new EMFLogItem();
        createTinyLogItem(logItem, id);
        return logItem;
    }


    public static EMFLogItem createLargeLogItem(EMFLogItem logItem, int id) {
        MetricsContext metricsContext = logItem.createMetricsContext();
        metricsContext.putDimension("dim1", "dimVal1");
        metricsContext.putDimension("dim2", "dimVal2");

        metricsContext.putMetric("metric1", 1);
        metricsContext.putMetric("metric2", 2);
        metricsContext.putProperty("stringProperty1", "simpleStringProp1");
        metricsContext.putProperty("intProperty1", 3);
        metricsContext.setNamespace("aNamespace");
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
        MetricsContext metricsContext = logItem.createMetricsContext();
        metricsContext.putDimension("dim1", "dimVal1");
        metricsContext.putDimension("dim2", "dimVal2");
        metricsContext.putMetric("metric1", 1);
        metricsContext.putMetric("metric2", 2);
        metricsContext.putProperty("stringProperty1", "simpleStringProp1");
        metricsContext.putProperty("intProperty1", 3);
        metricsContext.putProperty("complexProperty1", new ComplexProperty("aString", 4));
        metricsContext.setNamespace("aNamespace");
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
