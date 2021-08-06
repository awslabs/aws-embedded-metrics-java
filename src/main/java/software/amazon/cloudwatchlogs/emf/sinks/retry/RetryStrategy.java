package software.amazon.cloudwatchlogs.emf.sinks.retry;

public interface RetryStrategy {
    /**
     * Gets the amount of time to wait in millis before retrying
     *
     * @return millis to wait before retrying
     */
    int next();
}
