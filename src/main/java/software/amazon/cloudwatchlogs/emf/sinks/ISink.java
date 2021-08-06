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

package software.amazon.cloudwatchlogs.emf.sinks;

import java.util.concurrent.CompletableFuture;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

/** Interface for sinking log items to CloudWatch. */
public interface ISink {

    /**
     * Accept MetricsContext to sink to CloudWatch.
     *
     * @param context MetricsContext
     */
    void accept(MetricsContext context);

    /**
     * Shutdown the sink. The returned {@link CompletableFuture} will be completed when all queued
     * events have been flushed. After this is called, no more metrics can be sent through this sink
     * and attempting to continue to re-use the sink will result in undefined behavior.
     *
     * @return a future that completes when the shutdown has completed successfully and all pending
     *     messages have been sent to the destination.
     */
    CompletableFuture<Void> shutdown();
}
