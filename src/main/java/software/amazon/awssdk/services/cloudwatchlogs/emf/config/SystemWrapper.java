package software.amazon.awssdk.services.cloudwatchlogs.emf.config;

class SystemWrapper {
    static String getenv(String name) {
        return System.getenv(name);
    }
}
