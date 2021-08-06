package software.amazon.cloudwatchlogs.emf.sinks.retry;

import java.util.concurrent.ThreadLocalRandom;
import org.javatuples.Pair;

/**
 * A Fibonacci sequence with an upper bound. Once the upper limit is hit, all subsequent calls to
 * `next()` will return the provided limit.
 */
public class FibonacciRetryStrategy implements RetryStrategy {
    private final int maxJitter;
    private final int upperBound;

    Pair<Integer, Integer> cursor;

    public FibonacciRetryStrategy(int start, int upperBound, int maxJitter) {
        cursor = new Pair<>(start, start);
        this.upperBound = upperBound;
        this.maxJitter = maxJitter;
    }

    public int next() {
        int nextValue = cursor.getValue0() + cursor.getValue1();
        if (cursor.getValue1() >= upperBound) {
            cursor = new Pair<>(upperBound, upperBound);
        } else {
            cursor = new Pair<>(cursor.getValue1(), nextValue);
        }
        int jitter = maxJitter > 0 ? ThreadLocalRandom.current().nextInt(maxJitter) : 0;
        return cursor.getValue0() + jitter;
    }
}
