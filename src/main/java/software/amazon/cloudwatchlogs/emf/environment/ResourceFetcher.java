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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.javatuples.Pair;
import software.amazon.cloudwatchlogs.emf.exception.EMFClientException;
import software.amazon.cloudwatchlogs.emf.util.IOUtils;
import software.amazon.cloudwatchlogs.emf.util.Jackson;

@Slf4j
public class ResourceFetcher {

    /** Fetch a json object from a given uri and deserialize it to the specified class: clazz. */
    <T> T fetch(URI endpoint, Class<T> clazz) {
        String response = doReadResource(endpoint, "GET", Collections.emptyList());
        return Jackson.fromJsonString(response, clazz);
    }

    /**
     * Request a json object from a given uri with the provided headers and deserialize it to the
     * specified class: clazz.
     */
    <T> T fetch(URI endpoint, String method, Class<T> clazz, List<Pair<String, String>> headers) {
        String response = doReadResource(endpoint, method, headers);
        return Jackson.fromJsonString(response, clazz);
    }

    /** Request a string from a given uri with the provided headers */
    String fetch(URI endpoint, String method, List<Pair<String, String>> headers) {
        return doReadResource(endpoint, method, headers);
    }

    /**
     * Fetch a json object from a given uri and deserialize it to the specified class with a given
     * Jackson ObjectMapper.
     */
    <T> T fetch(URI endpoint, ObjectMapper objectMapper, Class<T> clazz) {
        String response = doReadResource(endpoint, "GET", Collections.emptyList());
        return Jackson.fromJsonString(response, objectMapper, clazz);
    }

    private String doReadResource(URI endpoint, String method, List<Pair<String, String>> headers) {
        InputStream inputStream = null;
        try {
            HttpURLConnection connection = connectToEndpoint(endpoint, method, headers);

            int statusCode = connection.getResponseCode();

            if (statusCode == HttpURLConnection.HTTP_OK) {
                inputStream = connection.getInputStream();
                return IOUtils.toString(inputStream);
            } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw new EMFClientException(
                        "The requested metadata is not found at " + connection.getURL());
            } else {
                inputStream = connection.getErrorStream();
                handleErrorResponse(inputStream, connection.getResponseMessage());
            }
        } catch (IOException ioException) {
            log.debug(
                    "An IOException occurred when connecting to service endpoint: "
                            + endpoint
                            + "\n Retrying to connect "
                            + "again.");
            throw new EMFClientException("Failed to connect to service endpoint: ", ioException);
        } finally {
            IOUtils.closeQuietly(inputStream, log);
        }
        return "";
    }

    private void handleErrorResponse(InputStream errorStream, String responseMessage)
            throws IOException {
        String errorCode = null;

        if (errorStream != null) {
            String errorResponse = IOUtils.toString(errorStream);

            try {
                JsonNode node = Jackson.jsonNodeOf(errorResponse);
                JsonNode code = node.get("code");
                JsonNode message = node.get("message");
                if (code != null && message != null) {
                    errorCode = code.asText();
                    responseMessage = message.asText();
                }

                String exceptionMessage =
                        String.format(
                                "Failed to get resource. Error code: %s, error message: %s ",
                                errorCode, responseMessage);
                throw new EMFClientException(exceptionMessage);

            } catch (Exception exception) {
                throw new EMFClientException("Unable to parse error stream: ", exception);
            }
        }
    }

    private HttpURLConnection connectToEndpoint(
            URI endpoint, String method, List<Pair<String, String>> headers) throws IOException {
        HttpURLConnection connection =
                (HttpURLConnection) endpoint.toURL().openConnection(Proxy.NO_PROXY);
        connection.setConnectTimeout(1000);
        connection.setReadTimeout(1000);
        connection.setRequestMethod(method);
        connection.setDoOutput(true);
        headers.forEach(
                header -> connection.setRequestProperty(header.getValue0(), header.getValue1()));

        connection.connect();

        return connection;
    }
}
