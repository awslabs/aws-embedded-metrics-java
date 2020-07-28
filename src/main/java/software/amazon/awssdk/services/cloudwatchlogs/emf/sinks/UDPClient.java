package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.Charset;

@Slf4j
class UDPClient implements SocketClient {

    private InetSocketAddress inetAddress;
    private DatagramSocket datagramSocket;
    private int port;

    public UDPClient(Endpoint endpoint) {
        inetAddress = new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
        port = endpoint.getPort();

        try {
            datagramSocket = new DatagramSocket();
        } catch (SocketException ex) {
            final String msg = "Could not instantiate DatagramSocket to " + endpoint.getHost();
            log.error(msg, ex);
        }
    }


    @Override
    public void sendMessage(String message) {
        byte[] data = message.getBytes(Charset.forName("UTF-8"));
        final DatagramPacket packet = new DatagramPacket(data,  data.length, inetAddress);
        flush(packet);
    }

    private synchronized void flush(DatagramPacket packet) {
        try {
            datagramSocket.send(packet);
        } catch (IOException ex) {
            final String msg = "Failed to send DatagramPacket to " + inetAddress;
            log.error(msg, ex);
        }
    }

    @Override
    public synchronized void close() throws IOException {
        if (datagramSocket != null) {
            datagramSocket.close();
            datagramSocket = null;
        }
    }
}
