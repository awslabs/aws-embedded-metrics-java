package software.amazon.cloudwatchlogs.emf.sinks.retry;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.Constants;

public class FibonacciRetryStrategyTest {
    @Test
    public void testDefaultRetryMatchesExpectedSequence() {
        // arrange
        int start = Constants.MIN_BACKOFF_MILLIS;
        int max = Constants.MAX_BACKOFF_MILLIS;
        FibonacciRetryStrategy strategy = new FibonacciRetryStrategy(start, max, 0);

        List<Integer> expectedSequence =
                Arrays.asList(50, 100, 150, 250, 400, 650, 1050, 1700, 2000);
        List<Integer> actualSequence = new ArrayList<>();

        // act
        int last = start;
        while (last < max) {
            last = strategy.next();
            actualSequence.add(last);
        }

        // assert
        assertEquals(expectedSequence, actualSequence);

        // verify subsequent calls don't exceed max bounds
        assertEquals(Constants.MAX_BACKOFF_MILLIS, strategy.next());
        assertEquals(Constants.MAX_BACKOFF_MILLIS, strategy.next());
    }
}
