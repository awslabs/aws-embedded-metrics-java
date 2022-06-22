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

package software.amazon.cloudwatchlogs.emf.exception;

import software.amazon.cloudwatchlogs.emf.Constants;

public class DimensionsExceededException extends RuntimeException {

    public DimensionsExceededException() {
        super(
                "Maximum number of dimensions allowed are "
                        + Constants.MAX_DIMENSIONS
                        + ". Account for default dimensions if not using setDimensions.");
    }

    public DimensionsExceededException(String message) {
        super(message);
    }
}
