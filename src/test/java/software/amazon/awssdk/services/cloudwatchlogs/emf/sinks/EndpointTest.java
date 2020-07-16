package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EndpointTest {

    @Test
    public void testParseTCPEndpoint() {
        String tcpEndpoint = "tcp://173.9.0.12:2580";
        Endpoint endpoint = Endpoint.fromURL(tcpEndpoint);

        assertEquals(endpoint.toString(), tcpEndpoint);
    }

    @Test
    public void testParseUDPEndpoint() {
        String tcpEndpoint = "udp://173.9.0.12:2580";
        Endpoint endpoint = Endpoint.fromURL(tcpEndpoint);

        assertEquals(endpoint.toString(), tcpEndpoint);
    }

    @Test
    public void testReturnDefaultEndpointForInvalidURI() {
        String unsupportedEndpoint = "http://173.9.0.12:2580";
        Endpoint endpoint = Endpoint.fromURL(unsupportedEndpoint);
        Endpoint endpointFromEmptyString = Endpoint.fromURL("");

        assertEquals(endpoint, Endpoint.DEFAULT_TCP_ENDPOINT);
        assertEquals(endpointFromEmptyString, Endpoint.DEFAULT_TCP_ENDPOINT);
    }
}
