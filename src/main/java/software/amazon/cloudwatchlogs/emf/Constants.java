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

package software.amazon.cloudwatchlogs.emf;

public class Constants {
    public static final int MAX_DIMENSION_SET_SIZE = 30;
    public static final short MAX_DIMENSION_NAME_LENGTH = 250;
    public static final short MAX_DIMENSION_VALUE_LENGTH = 1024;
    public static final short MAX_METRIC_NAME_LENGTH = 1024;
    public static final short MAX_NAMESPACE_LENGTH = 256;
    public static final String VALID_NAMESPACE_REGEX = "^[a-zA-Z0-9._#:/-]+$";
    public static final int MAX_TIMESTAMP_PAST_AGE_SECONDS = 60 * 60 * 24 * 14; // 14 days
    public static final int MAX_TIMESTAMP_FUTURE_AGE_SECONDS = 60 * 60 * 2; // 2 hours

    public static final int DEFAULT_AGENT_PORT = 25888;

    public static final String UNKNOWN = "Unknown";

    public static final int MAX_METRICS_PER_EVENT = 100;

    public static final int MAX_DATAPOINTS_PER_METRIC = 100;

    /**
     * The max number of messages to hold in memory in case of transient socket errors. The maximum
     * message size is 256 KB meaning the maximum size of this buffer would be 25.6 MB
     */
    public static final int DEFAULT_ASYNC_BUFFER_SIZE = 100;

    /**
     * How many times to retry an individual message. We eventually give up vs. retrying
     * indefinitely in case there is something inherent to the message that is causing the failures.
     * Giving up results in data loss, but also helps us reduce the risk of a poison pill blocking
     * all process telemetry.
     */
    public static final int MAX_ATTEMPTS_PER_MESSAGE = 100;

    /** Starting backoff millis when a transient socket failure is encountered. */
    public static final int MIN_BACKOFF_MILLIS = 50;

    /** Max backoff millis when a transient socket failure is encountered. */
    public static final int MAX_BACKOFF_MILLIS = 2000;

    /** Maximum amount of random jitter to apply to retries */
    public static final int MAX_BACKOFF_JITTER = 20;
}
