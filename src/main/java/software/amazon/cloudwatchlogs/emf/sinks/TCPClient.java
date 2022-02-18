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
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import lombok.extern.slf4j.Slf4j;

/** A client that would connect to a TCP socket. */
@Slf4j
public class TCPClient implements SocketClient {

    private final Endpoint endpoint;
    private Socket socket;
    private boolean shouldConnect = true;

    public TCPClient(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    private void connect() {
        try {
            socket = createSocket();
            socket.connect(new InetSocketAddress(endpoint.getHost(), endpoint.getPort()));
            shouldConnect = false;
        } catch (Exception e) {
            log.error("Failed to connect to the socket due to the exception: ", e);
            shouldConnect = true;
        }
    }

    protected Socket createSocket() {
        return new Socket();
    }

    @Override
    public synchronized void sendMessage(String message) {
        if (socket == null || socket.isClosed() || shouldConnect) {
            connect();
        }

        OutputStream os;
        try {
            os = socket.getOutputStream();
        } catch (IOException e) {
            log.error("Failed to open output stream: ", e);
            connect();
            return;
        }

        try {
            os.write(message.getBytes());
        } catch (IOException e) {
            log.error("Could not send write request due to IOException: ", e);
            connect();
        } catch (Exception e) {
            log.error("Could not send write request due to Exception: ", e);
            connect();
        }
    }

    @Override
    public void close() throws IOException {
        if (socket != null) {
            socket.close();
        }
    }
}
