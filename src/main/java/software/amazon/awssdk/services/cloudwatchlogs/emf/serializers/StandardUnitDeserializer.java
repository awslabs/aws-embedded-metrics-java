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

package software.amazon.awssdk.services.cloudwatchlogs.emf.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

/** JSON deserializer for StandardUnit type. */
public class StandardUnitDeserializer extends StdDeserializer<StandardUnit> {
    StandardUnitDeserializer() {
        this(null);
    }

    StandardUnitDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public StandardUnit deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException {

        String value = jp.getValueAsString();
        StandardUnit unit = StandardUnit.fromValue(value);
        return unit;
    }
}
