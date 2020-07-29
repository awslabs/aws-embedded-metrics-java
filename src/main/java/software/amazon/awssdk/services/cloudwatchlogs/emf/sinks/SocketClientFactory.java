package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

public class SocketClientFactory {

    public SocketClient getClient(Endpoint endpoint) {
        if (endpoint.getProtocol() == Protocol.UDP) {
            return new UDPClient(endpoint);
        }
        return new TCPClient(endpoint);
    }
}
