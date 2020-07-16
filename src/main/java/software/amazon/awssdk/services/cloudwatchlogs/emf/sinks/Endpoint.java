package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;


import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;


@Slf4j
@RequiredArgsConstructor
public class Endpoint {

    static public Endpoint DEFAULT_TCP_ENDPOINT = new Endpoint("0.0.0.0", 25888, Protocol.TCP);

    @Getter
    @NonNull
    private final String host;

    @Getter
    private final int port;

    @Getter
    @NonNull
    private final Protocol protocol;

    public static Endpoint fromURL(String endpoint) {
        URI parsedURI = null;

        try {
            parsedURI= new URI(endpoint);
        } catch (URISyntaxException ex) {
            log.warn("Failed to parse the endpoint: {} ", endpoint);
            return DEFAULT_TCP_ENDPOINT;
        }

        if (parsedURI.getHost() == null || parsedURI.getPort() < 0 || parsedURI.getScheme() == null) {
            return DEFAULT_TCP_ENDPOINT;
        }

        Protocol protocol;
        try {
            protocol = Protocol.getProtocol(parsedURI.getScheme());
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported protocol: {}. Would use default endpoint: {}", parsedURI.getScheme(), DEFAULT_TCP_ENDPOINT);
            return DEFAULT_TCP_ENDPOINT;
        }

        return new Endpoint(
                parsedURI.getHost(),
                parsedURI.getPort(),
                protocol
        );
    }

    public String toString() {
        return protocol.toString().toLowerCase() + "://" + host + ":" + port;
    }
}
