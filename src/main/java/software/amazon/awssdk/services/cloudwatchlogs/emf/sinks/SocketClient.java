package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import java.io.Closeable;

public interface SocketClient extends Closeable {

    /**
     * Send a message through the Socket Client
     * @param message The message to be sent
     */
    void sendMessage(String message);
}
