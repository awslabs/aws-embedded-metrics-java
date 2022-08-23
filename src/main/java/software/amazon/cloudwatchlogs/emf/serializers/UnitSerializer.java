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

package software.amazon.cloudwatchlogs.emf.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import software.amazon.cloudwatchlogs.emf.model.Unit;

/** JSON serializer for Unit type. */
public class UnitSerializer extends StdSerializer<Unit> {
    UnitSerializer() {
        this(null);
    }

    UnitSerializer(Class<Unit> t) {
        super(t);
    }

    @Override
    public void serialize(Unit value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        String str = value.toString();
        jgen.writeString(str);
    }
}
