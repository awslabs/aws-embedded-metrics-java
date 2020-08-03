package software.amazon.awssdk.services.cloudwatchlogs.emf.environment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.Optional;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.cloudwatchlogs.emf.Constants;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.Configuration;
import software.amazon.awssdk.services.cloudwatchlogs.emf.config.SystemWrapper;
import software.amazon.awssdk.services.cloudwatchlogs.emf.model.MetricsContext;
import software.amazon.awssdk.services.cloudwatchlogs.emf.util.StringUtils;

@Slf4j
class ECSEnvironment extends AgentBasedEnvironment {
    private Configuration config;
    private ECSMetadata metadata;
    private ResourceFetcher fetcher;
    private String fluentBitEndpoint;
    private String hostname;

    private static final String ECS_CONTAINER_METADATA_URI = "ECS_CONTAINER_METADATA_URI";
    private static final String FLUENT_HOST = "FLUENT_HOST";
    private static final String ENVIRONMENT_TYPE = "AWS::ECS::Container";

    ECSEnvironment(Configuration config, ResourceFetcher fetcher) {
        super(config);
        this.config = config;
        this.fetcher = fetcher;
    }

    @Override
    public boolean probe() {
        String uri = getEnv(ECS_CONTAINER_METADATA_URI);

        if (uri == null) {
            return false;
        }

        checkAndSetFluentHost();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        URI parsedURI = null;
        try {
            parsedURI = new URI(uri);
            metadata = fetcher.fetch(parsedURI, objectMapper, ECSMetadata.class);
            formatImageName();
            return true;
        } catch (Exception ex) {
            log.debug("Failed to get response from: " + parsedURI, ex);
        }

        return false;
    }

    @Override
    public String getName() {
        if (config.getServiceName().isPresent()) {
            return config.getServiceName().get();
        }
        if (metadata != null && !StringUtils.isNullOrEmpty(metadata.formattedImageName)) {
            return metadata.formattedImageName;
        }
        return Constants.UNKNOWN;
    }

    @Override
    public String getType() {
        return ENVIRONMENT_TYPE;
    }

    @Override
    public String getLogGroupName() {
        // FireLens / fluent-bit does not need the log group to be included
        // since configuration of the LogGroup is handled by the
        // fluent bit config file
        if (this.fluentBitEndpoint != null) {
            return "";
        }
        return super.getLogGroupName();
    }

    @Override
    public void configureContext(MetricsContext context) {

        context.putProperty("containerId", getHostName());
        context.putProperty("createdAt", metadata.createdAt);
        context.putProperty("startedAt", metadata.startedAt);
        context.putProperty("image", metadata.image);
        context.putProperty("cluster", metadata.labels.get("com.amazonaws.ecs.cluster"));
        context.putProperty("taskArn", metadata.labels.get("com.amazonaws.ecs.task-arn"));
    }

    private String getHostName() {
        if (hostname != null) {
            return hostname;
        }
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            log.debug("Unable to get hostname: ", ex);
        }
        return hostname;
    }

    private String getEnv(String name) {
        return SystemWrapper.getenv(name);
    }

    private void checkAndSetFluentHost() {
        String fluentHost = getEnv(FLUENT_HOST);
        if (fluentHost != null && !config.getAgentEndpoint().isPresent()) {
            fluentBitEndpoint =
                    String.format("tcp://%s:%d", fluentHost, Constants.DEFAULT_AGENT_PORT);
            config.setAgentEndpoint(Optional.of(fluentBitEndpoint));
            log.info("Using FluentBit configuration. Endpoint: {}", fluentBitEndpoint);
        }
    }

    private void formatImageName() {
        if (metadata != null && metadata.image != null) {
            String imageName = metadata.image;
            String[] splitImageNames = imageName.split("\\/");
            metadata.formattedImageName = splitImageNames[splitImageNames.length - 1];
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ECSMetadata {
        String name;
        String dockerId;
        String dockerName;
        String image;
        String formattedImageName;
        String imageID;
        String ports;
        Map<String, String> labels;
        String createdAt;
        String startedAt;
    }
}
