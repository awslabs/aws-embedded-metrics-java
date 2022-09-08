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

import static org.junit.Assert.*;

import org.junit.Test;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;

public class DimensionSetTest {
    @Test
    public void testAddDimension() throws DimensionSetExceededException {
        int dimensionsToBeAdded = 30;
        DimensionSet dimensionSet = generateDimensionSet(dimensionsToBeAdded);

        assertEquals(dimensionsToBeAdded, dimensionSet.getDimensionKeys().size());
    }

    @Test
    public void testAddDimensionLimitExceeded() {
        Exception exception =
                assertThrows(
                        DimensionSetExceededException.class,
                        () -> {
                            int dimensionSetSize = 33;
                            generateDimensionSet(dimensionSetSize);
                        });

        String expectedMessage = "Maximum number of dimensions";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void testMergeDimensionSets() {
        Exception exception =
                assertThrows(
                        DimensionSetExceededException.class,
                        () -> {
                            int dimensionSetSize = 28;
                            int otherDimensionSetSize = 5;
                            DimensionSet dimensionSet = generateDimensionSet(dimensionSetSize);
                            DimensionSet otherDimensionSet =
                                    generateDimensionSet(otherDimensionSetSize);
                            dimensionSet.add(otherDimensionSet);
                        });
        String expectedMessage = "Maximum number of dimensions";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    private DimensionSet generateDimensionSet(int numOfDimensions)
            throws DimensionSetExceededException {
        DimensionSet dimensionSet = new DimensionSet();

        for (int i = 0; i < numOfDimensions; i++) {
            dimensionSet.addDimension("Dimension" + i, "value" + i);
        }

        return dimensionSet;
    }
}
