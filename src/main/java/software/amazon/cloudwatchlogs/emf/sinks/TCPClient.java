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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import lombok.extern.slf4j.Slf4j;

/** A client that would connect to a TCP socket. */
@Slf4j
public class TCPClient implements SocketClient {

    private final Endpoint endpoint;
    private SocketChannel socketChannel;
    private boolean shouldConnect = true;

    private final ByteBuffer readBuffer = ByteBuffer.allocate(1);

    public TCPClient(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    private void connect() {
        try {
            socketChannel = SocketChannel.open();
            socketChannel.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
            shouldConnect = false;
        } catch (Exception e) {
            shouldConnect = true;
            throw new RuntimeException("Failed to connect to the socket.", e);
        }
    }

    @Override
    public synchronized void sendMessage(String message) {
        if (socketChannel == null || !socketChannel.isConnected() || shouldConnect) {
            connect();
        }

        try {
            socketChannel.configureBlocking(true);
            socketChannel.write(ByteBuffer.wrap(message.getBytes()));

            // Execute a non-blocking, single-byte read to detect if there was a connection closure.
            //   No actual data is expected to be read.
            readBuffer.clear();

            socketChannel.configureBlocking(false);
            socketChannel.read(readBuffer);

        } catch (Exception e) {
            shouldConnect = true;
            throw new RuntimeException("Failed to write message to the socket.", e);
        }
    }

    @Override
    public void close() throws IOException {
        if (socketChannel != null) {
            socketChannel.close();
        }
    }
}
