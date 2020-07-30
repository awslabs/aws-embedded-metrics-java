package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

@Slf4j
class UDPClient implements SocketClient {

    private InetSocketAddress inetAddress;
    private DatagramSocket datagramSocket;
    private Endpoint endpoint;

    UDPClient(Endpoint endpoint) {
        inetAddress = new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
        this.endpoint = endpoint;
    }


    @Override
    public void sendMessage(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        final DatagramPacket packet = new DatagramPacket(data,  data.length, inetAddress);
        flush(packet);
    }

    private synchronized void flush(DatagramPacket packet) {
        try {
            if (datagramSocket == null) {
                createSocket();
            }
            if (datagramSocket != null) {
                datagramSocket.send(packet);
            } else {
                log.warn("No DatagramSocket available. Message would be dropped");
            }
        } catch (IOException ex) {
            final String msg = "Failed to send DatagramPacket to " + inetAddress;
            log.error(msg, ex);
        }
    }

    @Override
    public void close() throws IOException {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }

    private void createSocket() {
        try {
            datagramSocket = new DatagramSocket();
        } catch (SocketException ex) {
            final String msg = "Could not instantiate DatagramSocket to " + endpoint.getHost();
            log.error(msg, ex);
        }
    }
}
