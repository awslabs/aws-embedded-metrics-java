package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import java.io.Closeable;

/** An interface for clients that connect to a socket. */
public interface SocketClient extends Closeable {

    /**
     * Send a message through the Socket Client.
     *
     * @param message The message to be sent
     */
    void sendMessage(String message);
}
