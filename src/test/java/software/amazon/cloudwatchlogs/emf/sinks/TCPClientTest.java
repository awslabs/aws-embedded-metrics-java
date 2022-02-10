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

    @Test
    public void testSendMessageWithGetOSException_THEN_createSocketTwice() throws IOException {
        Socket socket = mock(Socket.class);
        doNothing().when(socket).connect(any());
        when(socket.getOutputStream()).thenThrow(IOException.class);

        Endpoint endpoint = Endpoint.DEFAULT_TCP_ENDPOINT;
        TCPClient client =
                new TCPClient(endpoint) {
                    @Override
                    protected Socket createSocket() {
                        return socket;
                    }
                };

        TCPClient spyClient = spy(client);

        String message = "Test message";
        spyClient.sendMessage(message);
        verify(spyClient, atLeast(2)).createSocket();
    }

    @Test
    public void testSendMessageWithWriteOSException_THEN_createSocketTwice() throws IOException {
        Socket socket = mock(Socket.class);
        doNothing().when(socket).connect(any());
        ByteArrayOutputStream bos = mock(ByteArrayOutputStream.class);
        when(socket.getOutputStream()).thenReturn(bos);
        doThrow(IOException.class).when(bos).write(any());

        Endpoint endpoint = Endpoint.DEFAULT_TCP_ENDPOINT;
        TCPClient client =
                new TCPClient(endpoint) {
                    @Override
                    protected Socket createSocket() {
                        return socket;
                    }
                };

        TCPClient spyClient = spy(client);

        String message = "Test message";
        spyClient.sendMessage(message);
        verify(spyClient, atLeast(2)).createSocket();
    }
}
