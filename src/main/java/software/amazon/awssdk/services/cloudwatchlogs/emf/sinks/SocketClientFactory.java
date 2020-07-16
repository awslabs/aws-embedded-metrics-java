package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

public class SocketClientFactory {
    public SocketClient getClient(Endpoint endpoint) {
        if (endpoint.getProtocol() == Protocol.UDP) {
            //TODO: Replace with UDP client
            return null;
        }
        return new TCPClient(endpoint);
    }
}
