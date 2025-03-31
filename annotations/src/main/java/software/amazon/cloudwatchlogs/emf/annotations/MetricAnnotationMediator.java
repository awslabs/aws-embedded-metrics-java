/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
 */

package software.amazon.cloudwatchlogs.emf.annotations;

import java.util.HashMap;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;

/** */
public class MetricAnnotationMediator {
    public static MetricAnnotationMediator getInstance() {
        return SINGLETON;
    }

    private static final MetricAnnotationMediator SINGLETON = new MetricAnnotationMediator();

    private static final String defaultLoggerKey = "_defaultLogger";

    // protected instead of private for testing purposes
    static HashMap<String, MetricsLogger> loggers;

    private MetricAnnotationMediator() {
        loggers = new HashMap<>();
        loggers.put(defaultLoggerKey, new MetricsLogger());
    }

    /** @return the default logger this singleton uses */
    public static MetricsLogger getDefaultLogger() {
        return loggers.get(defaultLoggerKey);
    }

    /**
     * @param name the name of the logger to get
     * @return the logger with the specified name if it exists, otherwise will return the default
     *     logger
     * @see MetricAnnotationMediator#getDefaultLogger() getDefaultLogger()
     */
    public static MetricsLogger getLogger(String name) {
        if (name.isEmpty()) {
            return getDefaultLogger();
        }
        return loggers.getOrDefault(name, getDefaultLogger());
    }

    /**
     * Adds a logger to this singleton if no other logger has the same name.
     *
     * @param name the desired name of the logger
     * @param logger the logger to be added
     * @return true if the logger was successfully added and false if annother logger already has
     *     the same name
     */
    public static boolean addLogger(String name, MetricsLogger logger) {
        if (loggers.containsKey(name)) {
            return false;
        }

        loggers.put(name, logger);
        return true;
    }

    /** Flushes all loggers added to this singleton */
    public static void flushAll() {
        for (MetricsLogger logger : loggers.values()) {
            logger.flush();
        }
    }
}
