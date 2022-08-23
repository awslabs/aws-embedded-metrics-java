package software.amazon.cloudwatchlogs.emf.sinks;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CompletableFuture;

import lombok.Getter;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

public class MultiSinkTest {
    @Test
    public void shutdownClosesAllComponentSinks() {
        // arrange
        TestSink sink1 = new TestSink();
        TestSink sink2 = new TestSink();
        MultiSink multiSink = MultiSink.builder().sink(sink1).sink(sink2).build();

        // act
        CompletableFuture<Void> future = multiSink.shutdown();

        // assert
        assertTrue(future.isDone());
        assertEquals(1, sink1.getShutdowns());
        assertEquals(1, sink2.getShutdowns());
    }

    @Test
    public void shutdownCompletesExceptionallyIfComponentSinkCompletesExceptionally() {
        // arrange
        CompletableFuture<Void> failedResult =
                CompletableFuture.failedFuture(new RuntimeException());
        TestSink sink1 = new TestSink();
        TestSink sink2 = new TestSink(failedResult);
        MultiSink multiSink = MultiSink.builder().sink(sink1).sink(sink2).build();

        // act
        CompletableFuture<Void> future = multiSink.shutdown();

        // assert
        assertTrue(future.isDone());
        assertTrue(future.isCompletedExceptionally());
        assertEquals(1, sink1.getShutdowns());
        assertEquals(1, sink2.getShutdowns());
    }

    private static class TestSink implements ISink {
        private final CompletableFuture<Void> shutdownResult;
        @Getter
        int shutdowns = 0;

        TestSink() {
            this.shutdownResult = CompletableFuture.completedFuture(null);
        }

        TestSink(CompletableFuture<Void> shutdownResult) {
            this.shutdownResult = shutdownResult;
        }

        @Override
        public void accept(MetricsContext context) {
        }

        @Override
        public CompletableFuture<Void> shutdown() {
            shutdowns += 1;
            return shutdownResult;
        }
    }
}
