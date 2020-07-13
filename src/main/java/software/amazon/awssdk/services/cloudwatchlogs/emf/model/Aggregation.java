package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AccessLevel;
import lombok.Getter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashSet;
import java.util.Set;

/**
 * The Aggregation class is necessary to allow us to prevent the same aggregation from accidentally being created twice.
 * This allows us to wrap a single aggregation specification up into one object, then store these in a set where
 * every aggregation is unique.
 */
@JsonSerialize(using = AggregationSerializer.class)
@JsonDeserialize(using = AggregationDeserializer.class)
public class Aggregation {
    @JsonIgnore
    private RootNode rootNode;

    @Getter(AccessLevel.PACKAGE)
    private Set<String> dimensions = new HashSet<>();

    /**
     * Only used for deserialization for testing.
     * @param dimensions
     */
    Aggregation(String...dimensions) {
        for (String dimension : dimensions) {
            getDimensions().add(dimension);
        }
    }

    Aggregation(RootNode rootNode, String...dimensions) {
        this.rootNode = rootNode;
        for (String dimension : dimensions) {
            getDimensions().add(dimension);
        }
    }

    /**
     * Add another dimension to this aggregation.
     * @param dimension
     */
    public void addDimension(String dimension) {
        getDimensions().add(dimension);
    }

    /**
     * Add another dimension to this aggregation.
     * @param dimension
     */
    public void addDimension(String dimension, Object value) {
        getDimensions().add(dimension);
        setDimensionValue(dimension, value);
    }

    /**
     * Add a value for a dimension.
     * @param name
     * @param value
     */
    public void setDimensionValue(String name, Object value) {
        rootNode.setMetricOrProperty(name, value);
    }


    @Override
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        }

        if (o == this) {
            return true;
        }

        if (getClass() != o.getClass()) {
            return false;
        }

        Aggregation agg = (Aggregation) o;
        if (agg.getDimensions().size() != getDimensions().size()) {
            return false;
        }

        for (String dimension : getDimensions()) {
            if (!agg.getDimensions().contains(dimension)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hashCode = 0;
        for (String dimension : getDimensions()) {
            hashCode += dimension.hashCode();
        }
        return hashCode;
    }
}
