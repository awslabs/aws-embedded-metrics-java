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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

/**
 * Convenience helper for sinking the same log items to multiple destinations.
 *
 * <p>Useful for debugging, sink to CloudWatch and console.
 */
@Builder
public class MultiSink implements ISink {
    @Singular @NonNull private final List<ISink> sinks;

    @Override
    public void accept(MetricsContext context) {
        for (ISink sink : sinks) {
            sink.accept(context);
        }
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        @SuppressWarnings("rawtypes")
        final CompletableFuture[] list = new CompletableFuture[sinks.size()];
        for (int i = 0; i < sinks.size(); i++) {
            list[i] = sinks.get(i).shutdown();
        }
        return CompletableFuture.allOf(list);
    }
}
