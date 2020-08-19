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

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.Data;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;

/** A provider that will detect the environment. */
public class EnvironmentProvider {
    private static Environment cachedEnvironment;
    private final Configuration config = EnvironmentConfigurationProvider.getConfig();
    private final Environment lambdaEnvironment = new LambdaEnvironment();
    private final Environment defaultEnvironment = new DefaultEnvironment(config);
    private final Environment ec2Environment = new EC2Environment(config, new ResourceFetcher());
    private final Environment ecsEnvironment = new ECSEnvironment(config, new ResourceFetcher());

    // Ordering of this array matters
    private final Environment[] environments =
            new Environment[] {
                lambdaEnvironment, ecsEnvironment, ec2Environment, defaultEnvironment
            };

    public CompletableFuture<Environment> resolveEnvironment() {
        if (cachedEnvironment != null) {
            return CompletableFuture.completedFuture(cachedEnvironment);
        }

        Optional<Environment> env = getEnvironmentFromOverride();
        if (env.isPresent()) {
            cachedEnvironment = env.get();
            return CompletableFuture.completedFuture(cachedEnvironment);
        }

        CompletableFuture<Optional<EnvironmentResolveResult>> resolvedEnv =
                discoverEnvironmentAsync();

        return resolvedEnv.thenApply(
                optionalEnv ->
                        optionalEnv
                                .map(EnvironmentResolveResult::getEnvironment)
                                .orElseGet(
                                        () -> {
                                            cachedEnvironment = defaultEnvironment;
                                            return cachedEnvironment;
                                        }));
    }

    public Environment getDefaultEnvironment() {
        return defaultEnvironment;
    }

    /** A helper method to clean the cached environment in tests. */
    void cleanResolvedEnvironment() {
        cachedEnvironment = null;
    }

    private CompletableFuture<Optional<EnvironmentResolveResult>> discoverEnvironmentAsync() {

        CompletableFuture<Optional<EnvironmentResolveResult>> ans =
                CompletableFuture.completedFuture(Optional.empty());
        for (Environment env : environments) {
            CompletableFuture<EnvironmentResolveResult> future =
                    CompletableFuture.supplyAsync(
                            () -> new EnvironmentResolveResult(env.probe(), env));
            ans =
                    ans.thenCombine(
                            future,
                            (optionalEnv, envResult) -> {
                                if (optionalEnv.isPresent()) {
                                    return optionalEnv;
                                }
                                if (envResult.isCandidate) {
                                    return Optional.of(envResult);
                                }
                                return Optional.empty();
                            });
        }

        return ans;
    }

    private Optional<Environment> getEnvironmentFromOverride() {
        Configuration config = EnvironmentConfigurationProvider.getConfig();

        Optional<Environment> environment;
        switch (config.getEnvironmentOverride()) {
            case Lambda:
                environment = Optional.of(lambdaEnvironment);
                break;
            case Agent:
                environment = Optional.of(defaultEnvironment);
                break;
            case EC2:
                environment = Optional.of(ec2Environment);
                break;
            case ECS:
                environment = Optional.of(ecsEnvironment);
                break;
            case Local:
                environment = Optional.of(new LocalEnvironment(config));
                break;
            case Unknown:
            default:
                environment = Optional.empty();
        }
        return environment;
    }

    @AllArgsConstructor
    @Data
    static class EnvironmentResolveResult {
        private boolean isCandidate;
        private Environment environment;
    }
}
