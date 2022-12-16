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

package software.amazon.cloudwatchlogs.emf.model;

import java.util.stream.Stream;

public enum StorageResolution {
    STANDARD(60),
    HIGH(1),
    UNKNOWN_TO_SDK_VERSION(-1);

    private final int value;

    StorageResolution(final int newValue) {
        value = newValue;
    }

    public int getValue() {
        return this.value;
    }

    /**
     * Use this in place of valueOf to convert the raw integer returned by the service into the enum
     * value.
     *
     * @param value real value
     * @return Storage Resolution corresponding to the value
     */
    public static StorageResolution fromValue(int value) {
        return Stream.of(StorageResolution.values())
                .filter(e -> e.getValue() == value).findFirst().orElse(UNKNOWN_TO_SDK_VERSION);
    }
}