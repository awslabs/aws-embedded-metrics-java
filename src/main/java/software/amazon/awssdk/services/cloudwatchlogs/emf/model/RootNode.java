package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the root of the EMF schema.
 */
class RootNode {
    @Getter
    @JsonProperty("_aws")
    private Metadata aws = new Metadata();;
    private Map<String, Object> metricsAndProperties = new HashMap<>();

    @JsonAnySetter
    public void setMetricOrProperty(String key, Object value) {
        metricsAndProperties.put(key, value);
    }

    @JsonAnyGetter
    Map<String, Object> getMetricsAndProperties() {
        return metricsAndProperties;
    }
}
