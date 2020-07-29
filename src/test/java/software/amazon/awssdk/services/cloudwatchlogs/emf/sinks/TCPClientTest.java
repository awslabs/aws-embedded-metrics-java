package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.Socket;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class TCPClientTest {

    @Test
    public void testSendMessage() throws IOException {
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        when(socket.getOutputStream()).thenReturn(bos);
        doNothing().when(socket).connect(any());
        Endpoint endpoint = Endpoint.DEFAULT_TCP_ENDPOINT;

        TCPClient client = new TCPClient(endpoint) {
            @Override
            protected Socket createSocket() {
                return socket;
            }
        };

        String message = "Test message";
        client.sendMessage(message);

        assertEquals(bos.toString(), message);
    }
}
