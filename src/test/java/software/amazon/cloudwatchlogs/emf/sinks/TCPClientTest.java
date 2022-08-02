/*
 *   Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package software.amazon.cloudwatchlogs.emf.sinks;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import org.junit.Test;

public class TCPClientTest {

    @Test
    public void testSendMessage() throws IOException {
        Socket socket = mock(Socket.class);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        when(socket.getOutputStream()).thenReturn(bos);
        doNothing().when(socket).connect(any());
        Endpoint endpoint = Endpoint.DEFAULT_TCP_ENDPOINT;

        TCPClient client =
                new TCPClient(endpoint) {
                    @Override
                    protected Socket createSocket() {
                        return socket;
                    }
                };

        String message = "Test message";
        client.sendMessage(message);

        assertEquals(bos.toString(), message);
    }

    @Test(timeout = 5000)
    public void testSendMessageWithSocketServer() throws IOException {
        TCPClient client = new TCPClient(new Endpoint("0.0.0.0", 9999, Protocol.TCP));
        ServerSocket server = new ServerSocket(9999);
        client.sendMessage("Test message");
        Socket socket = server.accept();

        byte[] bytes = new byte[1024];
        int read = socket.getInputStream().read(bytes);
        String message = new String(bytes, 0, read);
        socket.close();
        server.close();

        assertEquals("Test message", message);
    }
}
