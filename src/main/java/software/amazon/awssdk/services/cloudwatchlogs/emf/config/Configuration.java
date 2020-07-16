package software.amazon.awssdk.services.cloudwatchlogs.emf.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import software.amazon.awssdk.services.cloudwatchlogs.emf.environment.Environments;

import java.util.Optional;

@AllArgsConstructor
public class Configuration {
    /**
     * Whether or not internal logging should be enabled.
     */
    @Getter @Setter
    boolean debuggingLoggingEnabled;

    /**
     * The name of the service to use in the default dimensions.
     */
    @Getter @Setter
    Optional<String> serviceName;

    /**
     * The type of the service to use in the default dimensions.
     */
    @Getter @Setter
    Optional<String> serviceType;

    /**
     * The LogGroup name to use. This will be ignored when using the
     * Lambda scope.
     */
    @Getter @Setter
    Optional<String> logGroupName;

    /**
     * The LogStream name to use. This will be ignored when using the
     * Lambda scope.
     */
    @Getter @Setter
    Optional<String> logStreamName;

    /**
     * The endpoint to use to connect to the CloudWatch Agent
     */
    @Getter @Setter
    Optional<String> agentEndpoint;

    /**
     * Environment override. This will short circuit auto-environment detection.
     * Valid values include:
     * - Local: no decoration and sends over stdout
     * - Lambda: decorates logs with Lambda metadata and sends over stdout
     * - Agent: no decoration and sends over TCP
     * - EC2: decorates logs with EC2 metadata and sends over TCP
     */
    @Getter @Setter
    Environments environmentOverride;
}
