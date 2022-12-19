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
import software.amazon.cloudwatchlogs.emf.model.StorageResolution;

public class StorageResolutionSerializer extends StdSerializer<StorageResolution> {
    StorageResolutionSerializer() {
        this(null);
    }

    StorageResolutionSerializer(Class<StorageResolution> t) {
        super(t);
    }

    @Override
    public void serialize(StorageResolution value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException {
        int resolution = value.getValue();
        jgen.writeNumber(resolution);
    }
}
