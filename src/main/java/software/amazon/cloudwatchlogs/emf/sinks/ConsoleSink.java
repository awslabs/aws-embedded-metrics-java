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

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.cloudwatchlogs.emf.model.MetricsContext;

/** Write log items to the console in JSON format. */
@Slf4j
@Builder
@NoArgsConstructor
public class ConsoleSink implements ISink {

    @Override
    public void accept(MetricsContext context) {

        try {
            // CHECKSTYLE OFF
            for (String event : context.serialize()) {
                System.out.println(event);
            }
            // CHECKSTYLE ON
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize a MetricsContext: ", e);
        }
    }
}
