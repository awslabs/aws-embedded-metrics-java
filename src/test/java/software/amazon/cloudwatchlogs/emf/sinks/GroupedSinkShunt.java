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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

/**
 * A mocked sink which can preserve all flushed log events. Useful for testing the result of
 * concurrent flushing.
 */
public class GroupedSinkShunt implements ISink {

    private List<MetricsContext> contexts = new ArrayList<>();

    private List<List<String>> logEventList = new ArrayList<>();

    @Override
    public void accept(MetricsContext context) {
        this.contexts.add(context);
        try {
            List<String> logEvent = context.serialize();
            logEventList.add(logEvent);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    public List<MetricsContext> getContexts() {
        return contexts;
    }

    public List<List<String>> getLogEventList() {
        return this.logEventList;
    }
}
