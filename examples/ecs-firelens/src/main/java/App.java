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

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;
import software.amazon.cloudwatchlogs.emf.environment.ECSEnvironment;
import software.amazon.cloudwatchlogs.emf.environment.Environment;
import software.amazon.cloudwatchlogs.emf.environment.EnvironmentProvider;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;
import sun.misc.Signal;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

public class App {

    private static final Environment env = new ECSEnvironment(EnvironmentConfigurationProvider.getConfig());

    public static void main(String[] args) throws Exception {
        registerShutdownHook();

        int portNumber = 8000;
        HttpServer server = HttpServer.create(new InetSocketAddress(8000), 0);
        System.out.println("Server started. Listening on " + portNumber);
        server.createContext("/", new SimpleHandler());
        server.setExecutor(null);
        server.start();
    }

    private static void registerShutdownHook() {
        // https://aws.amazon.com/blogs/containers/graceful-shutdowns-with-ecs/
        Signal.handle(new Signal("TERM"), sig -> {
            try {
                env.getSink().shutdown().get(1_000L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }

                e.printStackTrace();
            }

            System.exit(0);
        });
    }

    static class SimpleHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange he) throws IOException {
            long time = System.currentTimeMillis();
            String env = new EnvironmentProvider().resolveEnvironment().join().getClass().getName();
            String response = "from " + env + "\n";
            he.sendResponseHeaders(200, response.length());
            OutputStream os = he.getResponseBody();
            os.write(response.getBytes());
            os.close();

            MetricsLogger logger = new MetricsLogger();
            logger.putProperty("Method", he.getRequestMethod());
            logger.putProperty("Url", he.getRequestURI());
            logger.putMetric("ProcessingTime", System.currentTimeMillis() - time, Unit.MILLISECONDS);
            logger.flush();
            System.out.println(new EnvironmentProvider().resolveEnvironment().join().getClass().getName());

            // send application logs to stdout, FireLens will send this to a different LogGroup
            System.out.println("Completed request");
        }
    }
}
