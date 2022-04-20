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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class TCPClientTest {

    @Test
    public void testSendMessage() throws IOException {
        Endpoint endpoint = Endpoint.DEFAULT_TCP_ENDPOINT;
        InetSocketAddress socketAddress =
                new InetSocketAddress(endpoint.getHost(), endpoint.getPort());

        try (ServerSocketChannel serverListener = ServerSocketChannel.open()) {
            serverListener.bind(socketAddress);

            try (TCPClient client = new TCPClient(endpoint)) {
                String message = "Test message";
                client.sendMessage(message);

                byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
                ByteBuffer receiveBuffer = ByteBuffer.allocate(messageBytes.length);

                try (SocketChannel serverChannel = serverListener.accept()) {
                    serverChannel.read(receiveBuffer);
                }

                assertArrayEquals(receiveBuffer.array(), messageBytes);
            }
        }
    }

    @Test
    public void testDetectSocketClosure() throws IOException {
        Endpoint endpoint = Endpoint.DEFAULT_TCP_ENDPOINT;
        InetSocketAddress socketAddress =
                new InetSocketAddress(endpoint.getHost(), endpoint.getPort());

        try (ServerSocketChannel serverListener = ServerSocketChannel.open()) {
            serverListener.bind(socketAddress);

            try (TCPClient client = new TCPClient(endpoint)) {

                String message = "Test message";
                client.sendMessage(message);

                SocketChannel serverChannel = serverListener.accept();
                serverChannel.close();

                assertThrows(RuntimeException.class, () -> client.sendMessage(message));
            }
        }
    }
}
