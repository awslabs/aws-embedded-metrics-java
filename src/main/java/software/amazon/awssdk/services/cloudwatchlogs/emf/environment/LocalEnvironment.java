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

package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.Constants;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ConsoleSink;
import software.amazon.awssdk.services.cloudwatchlogs.emf.sinks.ISink;

@Slf4j
class LocalEnvironment implements Environment {
    private ISink sink;
    private Configuration config;

    LocalEnvironment(Configuration config) {
        this.config = config;
    }

    // probe is not intended to be used in the LocalEnvironment
    // To use the local environment you should set the environment
    // override
    @Override
    public boolean probe() {
        return false;
    }

    @Override
    public String getName() {
        if (config.getServiceName().isPresent()) {
            return config.getServiceName().get();
        }
        log.info("Unknown name");
        return Constants.UNKNOWN;
    }

    @Override
    public String getType() {
        if (config.getServiceType().isPresent()) {
            return config.getServiceType().get();
        }
        log.info("Unknown type");
        return Constants.UNKNOWN;
    }

    @Override
    public String getLogGroupName() {
        return config.getLogGroupName().orElse(getName() + "-metrics");
    }

    @Override
    public void configureContext(MetricsContext context) {
        // no-op
    }

    @Override
    public ISink getSink() {
        if (sink == null) {
            this.sink = new ConsoleSink();
        }
        return this.sink;
    }
}
