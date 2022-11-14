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
import lombok.extern.slf4j.Slf4j;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

@Slf4j
public class DefaultEnvironment extends AgentBasedEnvironment {
    private Configuration config;

    public DefaultEnvironment(Configuration config) {
        super(config);
        this.config = config;
    }

    @Override
    public boolean probe() {
        return true;
    }

    @Override
    public String getType() {
        Optional<String> serviceType = config.getServiceType();

        if (serviceType.isPresent()) {
            return serviceType.get();
        }

        log.info("Unknown ServiceType");
        return Constants.UNKNOWN;
    }

    @Override
    public void configureContext(MetricsContext context) {
        // no-op
    }
}
