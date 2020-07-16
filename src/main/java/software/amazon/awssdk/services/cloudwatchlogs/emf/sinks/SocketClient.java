package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

public interface SocketClient {

    /**
     * Establish a connection to a socket
     */
    void connect();

    /**
     * Send a message through the Socket Client
     * @param message The message to be sent
     */
    void sendMessage(String message);
}
