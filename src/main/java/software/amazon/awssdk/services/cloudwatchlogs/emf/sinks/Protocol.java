package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

public enum Protocol {
    TCP,
    UDP;

    public static Protocol getProtocol(String value) {
        for (Protocol protocol : values()) {
            if (protocol.toString().equalsIgnoreCase(value)) {
                return protocol;
            }
        }
        throw new IllegalArgumentException();
    }
}
