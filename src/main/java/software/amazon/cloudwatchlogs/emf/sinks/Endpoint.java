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

import java.net.URI;
import java.net.URISyntaxException;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class Endpoint {

    public static final Endpoint DEFAULT_TCP_ENDPOINT =
            new Endpoint("127.0.0.1", 25888, Protocol.TCP);

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
            parsedURI = new URI(endpoint);
        } catch (URISyntaxException ex) {
            log.warn("Failed to parse the endpoint: {} ", endpoint);
            return DEFAULT_TCP_ENDPOINT;
        }

        if (parsedURI.getHost() == null
                || parsedURI.getPort() < 0
                || parsedURI.getScheme() == null) {
            return DEFAULT_TCP_ENDPOINT;
        }

        Protocol protocol;
        try {
            protocol = Protocol.getProtocol(parsedURI.getScheme());
        } catch (IllegalArgumentException e) {
            log.warn(
                    "Unsupported protocol: {}. Would use default endpoint: {}",
                    parsedURI.getScheme(),
                    DEFAULT_TCP_ENDPOINT);

            return DEFAULT_TCP_ENDPOINT;
        }

        return new Endpoint(parsedURI.getHost(), parsedURI.getPort(), protocol);
    }

    public String toString() {
        return protocol.toString().toLowerCase() + "://" + host + ":" + port;
    }
}
