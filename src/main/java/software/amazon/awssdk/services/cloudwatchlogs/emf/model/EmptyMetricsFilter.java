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

package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

/**
 * A Jackson property filter that filters out "_aws" metadata object if no metrics have been added.
 */
class EmptyMetricsFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(
            Object pojo, JsonGenerator gen, SerializerProvider provider, PropertyWriter writer)
            throws Exception {
        if (include(writer)) {
            if (!writer.getName().equals("_aws")) {
                writer.serializeAsField(pojo, gen, provider);
                return;
            }
            Metadata metadata = ((RootNode) pojo).getAws();
            if (metadata.isEmpty()) {
                return;
            }
            writer.serializeAsField(pojo, gen, provider);
        } else if (!gen.canOmitFields()) {
            writer.serializeAsOmittedField(pojo, gen, provider);
        }
    }
}
