package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;

@Slf4j
public class TCPClient implements SocketClient {

    private Socket socket;
    private final Endpoint endpoint;
    private boolean shouldConnect = true;

    public TCPClient(Endpoint endpoint) {
        socket = createSocket();
        this.endpoint = endpoint;
    }

    private void connect() {
        try {
            socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
            shouldConnect = false;
        } catch (Exception e) {
            log.error("Failed to connect to the socket due to the exception: ", e);
            shouldConnect = true;
        }
    }

    protected Socket createSocket() {
        return new Socket();
    }

    @Override
    public synchronized void sendMessage(String message) {
        if (socket.isClosed() || shouldConnect) {
            connect();
        }

        OutputStream os;
        try{
            os = socket.getOutputStream();
        } catch (IOException e) {
            log.error("Failed to open output stream: ", e);
            connect();
            return;
        }

        try {
            os.write(message.getBytes());
        } catch (IOException e) {
            log.error("Could not send write request due to IOException: ", e);
            connect();
        } catch (Exception e) {
            log.error("Could not send write request due to Exception: ", e);
            connect();
        }
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }

}
