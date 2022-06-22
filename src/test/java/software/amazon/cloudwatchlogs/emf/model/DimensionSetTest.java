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

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.exception.DimensionsExceededException;

public class DimensionSetTest {
    @Test
    public void testAddDimensionLimitExceeded() {
        Exception exception =
                assertThrows(
                        DimensionsExceededException.class,
                        () -> {
                            DimensionSet dimensionSet = new DimensionSet();
                            int dimensionsToBeAdded = 33;

                            for (int i = 0; i < dimensionsToBeAdded; i++) {
                                dimensionSet.addDimension("Dimension" + i, "value" + i);
                            }
                        });
        String expectedMessage = "Maximum number of dimensions";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }
}
