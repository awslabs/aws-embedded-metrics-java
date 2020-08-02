package software.amazon.awssdk.services.cloudwatchlogs.emf.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;

/**
 * A Jackson property filter that filters out "_aws" metadata object if no metrics have been added.
 */
class EmptyMetricsFilter extends SimpleBeanPropertyFilter {

    @Override
    public void serializeAsField(
            Object pojo, JsonGenerator gen, SerializerProvider provider, PropertyWriter writer)
            throws Exception {
        if (include(writer)) {
            if (!writer.getName().equals("_aws")) {
                writer.serializeAsField(pojo, gen, provider);
                return;
            }
            Metadata metadata = ((RootNode) pojo).getAws();
            if (metadata.isEmpty()) {
                return;
            }
            writer.serializeAsField(pojo, gen, provider);
        } else if (!gen.canOmitFields()) {
            writer.serializeAsOmittedField(pojo, gen, provider);
        }
    }
}
