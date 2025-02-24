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

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.exception.EMFClientException;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.cloudwatchlogs.emf.sinks.retry.RetryStrategy;
import software.amazon.cloudwatchlogs.emf.util.StringUtils;

/** An sink connecting to an agent over a socket. */
@Slf4j
public class AgentSink implements ISink {
    private final String logGroupName;
    private final String logStreamName;
    private final SocketClient client;
    private final ExecutorService executor;
    private final Supplier<RetryStrategy> retryStrategyFactory;
    private final LinkedBlockingQueue<Runnable> queue;

    public AgentSink(
            String logGroupName,
            String logStreamName,
            Endpoint endpoint,
            SocketClientFactory clientFactory,
            int asyncQueueDepth,
            Supplier<RetryStrategy> retryStrategy) {
        this.logGroupName = logGroupName;
        this.logStreamName = logStreamName;
        client = clientFactory.getClient(endpoint);
        queue = new LinkedBlockingQueue<>(asyncQueueDepth);
        executor = createSingleThreadedExecutor();
        this.retryStrategyFactory = retryStrategy;
    }

    private ExecutorService createSingleThreadedExecutor() {
        return new ThreadPoolExecutor(
                1,
                1,
                0L,
                TimeUnit.MILLISECONDS,
                queue,
                new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void accept(MetricsContext context) {
        if (executor.isShutdown()) {
            throw new EMFClientException(
                    "Attempted to write data to a sink that has been previously shutdown.");
        }

        if (!StringUtils.isNullOrEmpty(logGroupName)) {
            context.putMetadata("LogGroupName", logGroupName);
        }

        if (!StringUtils.isNullOrEmpty(logStreamName)) {
            context.putMetadata("LogStreamName", logStreamName);
        }

        try {
            for (String event : context.serialize()) {
                executor.submit(new Sender(event, client, retryStrategyFactory));
            }
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize the metrics with the exception: ", e);
        }
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        executor.shutdown();
        return CompletableFuture.supplyAsync(
                () -> {
                    try {
                        while ((!executor.awaitTermination(1000, TimeUnit.MILLISECONDS))) {
                            // we add 1 because we assume that at least one task is running if the
                            // queue is blocked
                            log.debug(
                                    "Waiting for graceful shutdown to complete. {} tasks pending.",
                                    queue.size() + 1);
                        }
                    } catch (InterruptedException e) {
                        log.warn("Thread terminated while awaiting shutdown.");
                    }
                    return null;
                });
    }

    @AllArgsConstructor
    private static class Sender implements Runnable {
        private final String event;
        private final SocketClient client;
        private final Supplier<RetryStrategy> retryStrategyFactory;

        @Override
        public void run() {
            if (!StringUtils.isNullOrEmpty(event)) {
                try {
                    sendMessageForMaxAttempts();
                } catch (InterruptedException e) {
                    log.warn("Thread was interrupted while sending EMF event.");
                }
            }
        }

        private void sendMessageForMaxAttempts() throws InterruptedException {
            RetryStrategy backoff = null;

            for (int i = 0; i < Constants.MAX_ATTEMPTS_PER_MESSAGE; i++) {
                try {
                    client.sendMessage(event + "\n");
                    return;
                } catch (Exception e) {
                    log.debug(
                            "Failed to write the message to the socket. Backing off and trying again.",
                            e);
                    backoff = backoff != null ? backoff : retryStrategyFactory.get();
                    Thread.sleep(backoff.next());
                }
            }
            log.warn("Failed all attempts to write message to the socket.");
        }
    }
}
