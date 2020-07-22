package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ProtocolTest {

    @Test
    public void testParseTCP() {
        assertEquals(Protocol.getProtocol("TCP"), Protocol.TCP);
        assertEquals(Protocol.getProtocol("tcp"), Protocol.TCP);
        assertEquals(Protocol.getProtocol("Tcp"), Protocol.TCP);
    }

    @Test
    public void testParseUDP() {
        assertEquals(Protocol.getProtocol("UDP"), Protocol.UDP);
        assertEquals(Protocol.getProtocol("udp"), Protocol.UDP);
        assertEquals(Protocol.getProtocol("Udp"), Protocol.UDP);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowExceptionForUnsupportedProtocol() {
        Protocol.valueOf("http");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testThrowExceptionHttps() {
        Protocol.valueOf("https");
    }

}
