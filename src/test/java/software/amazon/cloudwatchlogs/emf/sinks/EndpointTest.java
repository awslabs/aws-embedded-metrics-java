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

import org.junit.Test;

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
