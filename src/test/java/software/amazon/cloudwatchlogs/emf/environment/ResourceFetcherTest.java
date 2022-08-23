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

package software.amazon.cloudwatchlogs.emf.environment;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.Assert.*;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;

import lombok.Data;
import org.javatuples.Pair;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.exception.EMFClientException;

public class ResourceFetcherTest {
    private ResourceFetcher fetcher;

    private static URI endpoint;
    private static final String endpoint_path = "/fake/endpoint";

    @ClassRule
    public static WireMockRule mockServer = new WireMockRule(0);

    @Before
    public void setUp() throws URISyntaxException {
        endpoint = new URI("http://localhost:" + mockServer.port() + endpoint_path);
        fetcher = new ResourceFetcher();
    }

    @Test
    public void testFetchThrowsExceptionWhenNoConnection() throws URISyntaxException {
        int port = 0;
        try {
            port = getUnusedPort();
        } catch (IOException ioexception) {
            fail("Unable to find an unused port");
        }

        try {
            fetcher.fetch(new URI("http://localhost:" + port), ResourceFetcher.class);
            fail("no exception is thrown");
        } catch (EMFClientException exception) {
            assertTrue(exception.getMessage().contains("Failed to connect"));
        }
    }

    @Test
    public void testFetchThrowsExceptionFor404Response() throws Exception {
        generateStub(404, "NotFound");
        try {
            fetcher.fetch(endpoint, TestData.class);
            fail("Expected EMFClientException");
        } catch (EMFClientException ex) {
            assertTrue(ex.getMessage().contains("not found"));
        }
    }

    @Test
    public void testFetchThrowsExceptionFor500Response() {
        generateStub(500, "ServerError");
        try {
            fetcher.fetch(endpoint, TestData.class);
            fail("Expected EMFClientException");
        } catch (EMFClientException ex) {
            assertTrue(ex.getMessage().contains("Unable to parse error stream"));
        }
    }

    @Test
    public void testReadDataWith200Response() {
        generateStub(200, "{\"name\":\"test\",\"size\":10}");
        TestData data = fetcher.fetch(endpoint, TestData.class);

        assertEquals("test", data.name);
        assertEquals(10, data.size);
    }

    @Test
    public void testReadDataWithHeaders200Response() {
        Pair<String, String> mockHeader = new Pair<>("X-mock-header-key", "headerValue");
        generateStub(200, "{\"name\":\"test\",\"size\":10}");
        TestData data =
                fetcher.fetch(
                        endpoint, "GET", TestData.class, Collections.singletonList(mockHeader));

        verify(
                getRequestedFor(urlEqualTo(endpoint_path))
                        .withHeader("X-mock-header-key", equalTo("headerValue")));
        assertEquals("test", data.name);
        assertEquals(10, data.size);
    }

    @Test
    public void testWithProvidedMethodAndHeadersWith200Response() {
        generatePutStub(200, "putResponseData");
        Pair<String, String> mockHeader = new Pair<>("X-mock-header-key", "headerValue");
        String data = fetcher.fetch(endpoint, "PUT", Collections.singletonList(mockHeader));

        verify(
                putRequestedFor(urlEqualTo(endpoint_path))
                        .withHeader("X-mock-header-key", equalTo("headerValue")));
        assertEquals("putResponseData", data);
    }

    @Test
    public void testReadCaseInsensitiveDataWith200Response() {
        generateStub(200, "{\"Name\":\"test\",\"Size\":10}");
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        TestData data = fetcher.fetch(endpoint, objectMapper, TestData.class);

        assertEquals("test", data.name);
        assertEquals(10, data.size);
    }

    @Test
    public void testReadDataWith200ResponseButInvalidJson() {

        generateStub(200, "error");
        try {
            fetcher.fetch(endpoint, TestData.class);
            fail("Expected EMFClientException");
        } catch (EMFClientException ex) {
            assertTrue(ex.getMessage().contains("Unable to parse Json String"));
        }
    }

    static int getUnusedPort() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        socket.setReuseAddress(true);
        int port = socket.getLocalPort();
        socket.close();
        return port;
    }

    private void generateStub(int statusCode, String message) {
        stubFor(
                get(urlPathEqualTo(endpoint_path))
                        .willReturn(
                                aResponse()
                                        .withStatus(statusCode)
                                        .withHeader("Content-Type", "application/json")
                                        .withHeader("charset", "utf-8")
                                        .withBody(message)));
    }

    private void generatePutStub(int statusCode, String message) {
        stubFor(
                put(urlPathEqualTo(endpoint_path))
                        .willReturn(
                                aResponse()
                                        .withStatus(statusCode)
                                        .withHeader("Content-Type", "application/json")
                                        .withHeader("charset", "utf-8")
                                        .withBody(message)));
    }

    @Data
    private static class TestData {
        private String name;
        private int size;
    }
}
