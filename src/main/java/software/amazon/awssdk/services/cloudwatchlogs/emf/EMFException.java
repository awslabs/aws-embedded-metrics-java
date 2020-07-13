package software.amazon.awssdk.services.cloudwatchlogs.emf;

/**
 * Exception base for EMF.
 */
public class EMFException extends Exception {
    public EMFException(String message, Throwable cause) {
        super(message, cause);
    }

    public EMFException(String message) {
        super(message);
    }

    public EMFException(Throwable cause) {
        super(cause);
    }

    public EMFException() { }
}
