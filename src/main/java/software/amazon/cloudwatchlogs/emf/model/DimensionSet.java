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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import software.amazon.cloudwatchlogs.emf.Constants;
import software.amazon.cloudwatchlogs.emf.exception.DimensionSetExceededException;
import software.amazon.cloudwatchlogs.emf.exception.InvalidDimensionException;
import software.amazon.cloudwatchlogs.emf.util.Validator;

/** A combination of dimension values. */
@ToString
@EqualsAndHashCode
public class DimensionSet {

    @Getter(AccessLevel.PACKAGE)
    private final Map<String, String> dimensionRecords = new LinkedHashMap<>();

    /**
     * Return a dimension set that contains a single pair of key-value.
     *
     * @param d1 Name of the single dimension
     * @param v1 Value of the single dimension
     * @return a DimensionSet from the parameters
     * @throws InvalidDimensionException if the dimension name or value is invalid
     */
    public static DimensionSet of(String d1, String v1) throws InvalidDimensionException {
        return fromEntries(entryOf(d1, v1));
    }

    /**
     * Return a dimension set that contains two entries.
     *
     * @param d1 Name of the first dimension
     * @param v1 Value of the first dimension
     * @param d2 Name of the second dimension
     * @param v2 Value of the second dimension
     * @return a DimensionSet from the parameters
     * @throws InvalidDimensionException if the dimension name or value is invalid
     */
    public static DimensionSet of(String d1, String v1, String d2, String v2)
            throws InvalidDimensionException {
        return fromEntries(entryOf(d1, v1), entryOf(d2, v2));
    }

    /**
     * Return a dimension set that contains three entries.
     *
     * @param d1 Name of the first dimension
     * @param v1 Value of the first dimension
     * @param d2 Name of the second dimension
     * @param v2 Value of the second dimension
     * @param d3 Name of the third dimension
     * @param v3 Value of the third dimension
     * @return a DimensionSet from the parameters
     * @throws InvalidDimensionException if the dimension name or value is invalid
     */
    public static DimensionSet of(String d1, String v1, String d2, String v2, String d3, String v3)
            throws InvalidDimensionException {
        return fromEntries(entryOf(d1, v1), entryOf(d2, v2), entryOf(d3, v3));
    }

    /**
     * Return a dimension set that contains four entries.
     *
     * @param d1 Name of the first dimension
     * @param v1 Value of the first dimension
     * @param d2 Name of the second dimension
     * @param v2 Value of the second dimension
     * @param d3 Name of the third dimension
     * @param v3 Value of the third dimension
     * @param d4 Name of the fourth dimension
     * @param v4 Value of the fourth dimension
     * @return a DimensionSet from the parameters
     * @throws InvalidDimensionException if the dimension name or value is invalid
     */
    public static DimensionSet of(
            String d1, String v1, String d2, String v2, String d3, String v3, String d4, String v4)
            throws InvalidDimensionException {

        return fromEntries(entryOf(d1, v1), entryOf(d2, v2), entryOf(d3, v3), entryOf(d4, v4));
    }

    /**
     * Return a dimension set that contains five entries.
     *
     * @param d1 Name of the first dimension
     * @param v1 Value of the first dimension
     * @param d2 Name of the second dimension
     * @param v2 Value of the second dimension
     * @param d3 Name of the third dimension
     * @param v3 Value of the third dimension
     * @param d4 Name of the fourth dimension
     * @param v4 Value of the fourth dimension
     * @param d5 Name of the fifth dimension
     * @param v5 Value of the fifth dimension
     * @return a DimensionSet from the parameters
     * @throws InvalidDimensionException if the dimension name or value is invalid
     */
    public static DimensionSet of(
            String d1,
            String v1,
            String d2,
            String v2,
            String d3,
            String v3,
            String d4,
            String v4,
            String d5,
            String v5)
            throws InvalidDimensionException {

        return fromEntries(
                entryOf(d1, v1),
                entryOf(d2, v2),
                entryOf(d3, v3),
                entryOf(d4, v4),
                entryOf(d5, v5));
    }

    private static DimensionSet fromEntries(DimensionEntry... entries)
            throws InvalidDimensionException, DimensionSetExceededException {
        DimensionSet ds = new DimensionSet();
        for (DimensionEntry entry : entries) {
            ds.addDimension(entry.key, entry.value);
        }
        return ds;
    }

    private static DimensionEntry entryOf(String key, String value) {
        return new DimensionEntry(key, value);
    }

    /**
     * Add another dimension entry to this DimensionSet.
     *
     * @param dimension Name of the dimension
     * @param value Value of the dimension
     * @throws InvalidDimensionException if the dimension name or value is invalid
     * @throws DimensionSetExceededException if the number of dimensions exceeds the limit
     */
    public void addDimension(String dimension, String value)
            throws InvalidDimensionException, DimensionSetExceededException {
        Validator.validateDimensionSet(dimension, value);

        if (this.getDimensionKeys().size() >= Constants.MAX_DIMENSION_SET_SIZE) {
            throw new DimensionSetExceededException();
        }

        this.getDimensionRecords().put(dimension, value);
    }

    /**
     * Add a dimension set with current dimension set and return a new dimension set from combining
     * the two dimension sets.
     *
     * @param other Other dimension sets to merge with current
     * @return a new DimensionSet from combining the current DimensionSet with other
     * @throws DimensionSetExceededException if the number of dimensions exceeds the limit
     */
    public DimensionSet add(DimensionSet other) throws DimensionSetExceededException {
        DimensionSet mergedDimensionSet = new DimensionSet();
        int mergedDimensionSetSize =
                this.getDimensionKeys().size() + other.dimensionRecords.keySet().size();
        if (mergedDimensionSetSize > Constants.MAX_DIMENSION_SET_SIZE) {
            throw new DimensionSetExceededException();
        }

        mergedDimensionSet.dimensionRecords.putAll(dimensionRecords);
        mergedDimensionSet.dimensionRecords.putAll(other.dimensionRecords);
        return mergedDimensionSet;
    }

    /** @return all the dimension names in the dimension set. */
    public Set<String> getDimensionKeys() {
        return dimensionRecords.keySet();
    }

    /**
     * @param key the name of the dimension
     * @return the dimension value associated with a dimension key.
     */
    public String getDimensionValue(String key) {
        return this.dimensionRecords.get(key);
    }

    @AllArgsConstructor
    static class DimensionEntry {
        @Getter private String key;
        @Getter private String value;
    }
}
