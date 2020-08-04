package software.amazon.awssdk.services.cloudwatchlogs.emf.exception;

public class EMFClientException extends RuntimeException {

    public EMFClientException(String message, Throwable t) {
        super(message, t);
    }

    public EMFClientException(String message) {
        super(message);
    }
}
