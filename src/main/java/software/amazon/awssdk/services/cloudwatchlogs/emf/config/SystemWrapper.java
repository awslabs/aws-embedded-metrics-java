package software.amazon.awssdk.services.cloudwatchlogs.emf.config;

/** A wrapper class that can be used to mock 'System.getenv' with PowerMock. */
public class SystemWrapper {

    public static String getenv(String name) {
        return System.getenv(name);
    }
}
