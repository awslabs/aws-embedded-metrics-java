package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * A combination of dimension values
 */
public class DimensionSet {

    @Getter(AccessLevel.PACKAGE)
    private Map<String, String> dimensionRecords = new LinkedHashMap<>();

    /**
     * Return a dimension set that contains a single pair of key-value
     *
     * @param d1 Name of the single dimension
     * @param v1 Value of the single dimension
     */
    public static DimensionSet of(String d1, String v1) {
        return fromEntries(entryOf(d1, v1));
    }

    /**
     * Return a dimension set that contains two entries
     *
     * @param d1 Name of the first dimension
     * @param v1 Value of the first dimension
     * @param d2 Name of the second dimension
     * @param v2 Value of the second dimension
     */
    public static DimensionSet of(String d1, String v1, String d2, String v2) {
        return fromEntries(entryOf(d1, v1), entryOf(d2, v2));
    }


    /**
     * Return a dimension set that contains three entries
     *
     * @param d1 Name of the first dimension
     * @param v1 Value of the first dimension
     * @param d2 Name of the second dimension
     * @param v2 Value of the second dimension
     * @param d3 Name of the third dimension
     * @param v3 Value of the third dimension
     */
    public static DimensionSet of(String d1, String v1, String d2, String v2, String d3, String v3) {
        return fromEntries(entryOf(d1, v1), entryOf(d2, v2), entryOf(d3, v3));

    }

    /**
     * Return a dimension set that contains four entries
     *
     * @param d1 Name of the first dimension
     * @param v1 Value of the first dimension
     * @param d2 Name of the second dimension
     * @param v2 Value of the second dimension
     * @param d3 Name of the third dimension
     * @param v3 Value of the third dimension
     * @param d4 Name of the fourth dimension
     * @param v4 Value of the fourth dimension
     */
    public static DimensionSet of(String d1, String v1, String d2, String v2, String d3, String v3, String d4, String v4) {
        return fromEntries(entryOf(d1, v1), entryOf(d2, v2), entryOf(d3, v3), entryOf(d4, v4));

    }

    /**
     * Return a dimension set that contains five entries
     *
     * @param d1 Name of the first dimension
     * @param v1 Value of the first dimension
     * @param d2 Name of the second dimension
     * @param v2 Value of the second dimension
     * @param d3 Name of the third dimension
     * @param v3 Value of the third dimension
     * @param d4 Name of the fourth dimension
     * @param v4 Value of the fourth dimension
     * @param d4 Name of the fifth dimension
     * @param v4 Value of the fifth dimension
     */
    public static DimensionSet of(String d1, String v1, String d2, String v2, String d3, String v3, String d4, String v4, String d5, String v5) {
        return fromEntries(entryOf(d1, v1), entryOf(d2, v2), entryOf(d3, v3), entryOf(d4, v4), entryOf(d5, v5));

    }

    private static DimensionSet fromEntries(DimensionEntry...entries) {
        DimensionSet ds = new DimensionSet();
        for (DimensionEntry entry : entries) {
            ds.addDimension(entry.key, entry.value);
        }
        return ds;
    }


    @AllArgsConstructor
    static class DimensionEntry {
        @Getter
        private String key;
        @Getter
        private String value;
    }

    private static DimensionEntry entryOf(String key, String value) {
        return new DimensionEntry(key, value);
    }

    /**
     * Add another dimension entry to this DimensionSet.
     * @param dimension Name of the dimension
     * @param value Value of the dimension
     */
    public void addDimension(String dimension, String value) {
        this.getDimensionRecords().put(dimension, value);
    }

    /**
     * Add a dimension set with current dimension set and return a new dimension set from combining the two dimension sets
     * @param other Other dimension sets to merge with current
     */
    public DimensionSet add(DimensionSet other) {
        DimensionSet mergedDimensionSet = new DimensionSet();
        mergedDimensionSet.dimensionRecords.putAll(dimensionRecords);
        mergedDimensionSet.dimensionRecords.putAll(other.dimensionRecords);
        return mergedDimensionSet;
    }

    /**
     *
     * @return The dimension names in the dimension set
     */
    public Set<String> getDimensionKeys() {
        return dimensionRecords.keySet();
    }
}
