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

package software.amazon.awssdk.services.cloudwatchlogs.emf.sinks;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;

/** A client that would connect to a UDP socket. */
@Slf4j
class UDPClient implements SocketClient {

    private InetSocketAddress inetAddress;
    private DatagramSocket datagramSocket;
    private Endpoint endpoint;

    UDPClient(Endpoint endpoint) {
        inetAddress = new InetSocketAddress(endpoint.getHost(), endpoint.getPort());
        this.endpoint = endpoint;
    }

    @Override
    public void sendMessage(String message) {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        final DatagramPacket packet = new DatagramPacket(data, data.length, inetAddress);
        flush(packet);
    }

    private synchronized void flush(DatagramPacket packet) {
        try {
            if (datagramSocket == null) {
                createSocket();
            }
            if (datagramSocket != null) {
                datagramSocket.send(packet);
            } else {
                log.warn("No DatagramSocket available. Message would be dropped");
            }
        } catch (IOException ex) {
            final String msg = "Failed to send DatagramPacket to " + inetAddress;
            log.error(msg, ex);
        }
    }

    @Override
    public void close() throws IOException {
        if (datagramSocket != null) {
            datagramSocket.close();
        }
    }

    private void createSocket() {
        try {
            datagramSocket = new DatagramSocket();
        } catch (SocketException ex) {
            final String msg = "Could not instantiate DatagramSocket to " + endpoint.getHost();
            log.error(msg, ex);
        }
    }
}
