## aws-embedded-metrics

![](https://codebuild.us-west-2.amazonaws.com/badges?uuid=eyJlbmNyeXB0ZWREYXRhIjoiNFp3emNQV0dUbWg5bHBqbXZsMjlOY0dZN0xFTlc3aVhQV1dnVW1uS1kxU3FINlpmRTlIYjNQdHRkcVVvM1RNK3ZLQ25qRHZkK1pBTFIxWFUwaU1NcktjPSIsIml2UGFyYW1ldGVyU3BlYyI6InFzblFPZGgzWXF2V2V5OFYiLCJtYXRlcmlhbFNldFNlcmlhbCI6MX0%3D&branch=master)

Generate CloudWatch metrics embedded within structured log events. The embedded metrics will be extracted so that you can visualize and alarm on them for real-time incident detection. This allows you to monitor aggregated values while preserving the detailed log event context that generates them.
- [Use Cases](#use-cases)
- [Usage](#usage)
- [API](#api)
- [Examples](#examples)
- [Development](#development)

## Use Cases

- **Generate custom metrics across compute environments**

	- Easily generate custom metrics from Lambda functions without requiring custom batching code, making blocking network requests or relying on third-party software.
	- Other compute environments (EC2, On-prem, ECS, EKS, and other container environments) are supported by installing the [CloudWatch Agent](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/CloudWatch_Embedded_Metric_Format_Generation_CloudWatch_Agent.html).

- **Link metrics to high cardinality context**

	Using the Embedded Metric Format, you will be able to visualize and alarm on custom metrics, but also retain the original, detailed and high-cardinality context which is queryable using [CloudWatch Logs Insights](https://docs.aws.amazon.com/AmazonCloudWatch/latest/logs/AnalyzingLogData.html). For example, the library automatically injects environment metadata such as Lambda Function version, EC2 instance and image ids into the structured log event data.

## Usage

To use a metric logger, you need to manually create and flush the logger.


```java
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;
import software.amazon.cloudwatchlogs.emf.model.Unit;

class Example {
	public static void main(String[] args) {
		MetricsLogger logger = new MetricsLogger();
		metrics.putDimensions(DimensionSet.of("Service", "Aggregator"));
		metrics.putMetric("ProcessingLatency", 100, Unit.Milliseconds);
		metrics.setProperty("RequestId", "422b1569-16f6-4a03-b8f0-fe3fd9b100f8");
		logger.flush();
	}
}
```

You can find the artifact location and examples of how to include it in your project at [Maven Central](https://search.maven.org/artifact/software.amazon.cloudwatchlogs/aws-embedded-metrics):

https://search.maven.org/artifact/software.amazon.cloudwatchlogs/aws-embedded-metrics

## API

### MetricsLogger

The `MetricsLogger` is the interface you will use to publish embedded metrics.

- MetricsLogger **putMetric**(String key, double value, Unit unit)
- MetricsLogger **putMetric**(String key, double value)

Adds a new metric to the current logger context. Multiple metrics using the same key will be appended to an array of values. The Embedded Metric Format supports a maximum of 100 values per key.

Requirements:

- Name Length 1-255 characters
- Name must be ASCII characters only
- Values must be in the range of 8.515920e-109 to 1.174271e+108. In addition, special values (for example, NaN, +Infinity, -Infinity) are not supported.
- Units must meet CloudWatch Metrics unit requirements, if not it will default to None. See [MetricDatum](https://docs.aws.amazon.com/AmazonCloudWatch/latest/APIReference/API_MetricDatum.html) for valid values.

Examples:

```java
putMetric("Latency", 200, Unit.MILLISECONDS)
```

- MetricsLogger **putProperty**(String key, Object value )

Adds or updates the value for a given property on this context. This value is not submitted to CloudWatch Metrics but is searchable by CloudWatch Logs Insights. This is useful for contextual and potentially high-cardinality data that is not appropriate for CloudWatch Metrics dimensions.

Requirements:

- Length 1-255 characters

Examples:

```java
putProperty("RequestId", "422b1569-16f6-4a03-b8f0-fe3fd9b100f8")
putProperty("InstanceId", "i-1234567890")
putProperty("Device", new HashMap<String, String>() {{
		put("Id", "61270781-c6ac-46f1-baf7-22c808af8162");
		put("Name", "Transducer");
		put("Model", "PT-1234");
	}}
);
```

- MetricsLogger **putDimensions**(DimensionSet dimensions)

Adds a new set of dimensions that will be associated with all metric values.

**WARNING**: Every distinct value for a dimension set will result in a new CloudWatch metric.
If the cardinality of a particular value is expected to be high, you should consider
using `setProperty` instead.

Requirements:

- Length 1-255 characters
- ASCII characters only

Examples:

```java
putDimensions(DimensionSet.of("Operation", "Aggregator"))
putDimensions(DimensionSet.of("Operation", "Aggregator", "DeviceType", "Actuator"))
```

- MetricsLogger **setDimensions**(DimensionSet dimensions)

Explicitly override all dimensions. This will remove the default dimensions.

**WARNING**: Every distinct value for a dimension set will result in a new CloudWatch metric.
If the cardinality of a particular value is expected to be high, you should consider
using `setProperty` instead.

Requirements:

- Length 1-255 characters
- ASCII characters only

Examples:

```java
setDimensions(DimensionSet.of(
	"Operation", "Aggregator",
	"DeviceType", "Actuator")
)

```

- MetricsLogger **setNamespace**(String value)

Sets the CloudWatch [namespace](https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/cloudwatch_concepts.html#Namespace) that extracted metrics should be published to. If not set, a default value of aws-embedded-metrics will be used.

Requirements:

- Name Length 1-255 characters
- Name must be ASCII characters only

Examples:

```java
setNamespace("MyApplication")
```

- **flush**()

Flushes the current MetricsContext to the configured sink and resets all properties, dimensions and metric values. The namespace and default dimensions will be preserved across flushes.

### Configuration

All configuration values can be set using environment variables with the prefix (`AWS_EMF_`). Configuration should be performed as close to application start up as possible.

**ServiceName**: Overrides the name of the service. For services where the name cannot be inferred (e.g. Java process running on EC2), a default value of Unknown will be used if not explicitly set.

Requirements:

- Name Length 1-255 characters
- Name must be ASCII characters only

Example:

```java
# in process
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;

Configuration config = EnvironmentConfigurationProvider.getConfig();
config.setServiceName("MyApp")

# environment
AWS_EMF_SERVICE_NAME="MyApp"
```

**ServiceType**: Overrides the type of the service. For services where the type cannot be inferred (e.g. Java process running on EC2), a default value of Unknown will be used if not explicitly set.

Requirements:

- Name Length 1-255 characters
- Name must be ASCII characters only

Example:

```java
# in process
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;

Configuration config = EnvironmentConfigurationProvider.getConfig();
config.setServiceType("JavaWebApp")

# environment
AWS_EMF_SERVICE_TYPE="JavaWebApp"
```

**LogGroupName**: For agent-based platforms, you may optionally configure the destination log group that metrics should be delivered to. This value will be passed from the library to the agent in the Embedded Metric payload. If a LogGroup is not provided, the default value will be derived from the service name: <service-name>-metrics

Requirements:

- Name Length 1-512 characters
- Log group names consist of the following characters: a-z, A-Z, 0-9, '\_' (underscore), '-' (hyphen), '/' (forward slash), and '.' (period). Pattern: [\.\-_/#A-Za-z0-9]+

Example:

```java
# in process
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;

Configuration config = EnvironmentConfigurationProvider.getConfig();
config.setLogGroupName("LogGroupName")

# environment
AWS_EMF_LOG_GROUP_NAME="LogGroupName"
```

**LogStreamName**: For agent-based platforms, you may optionally configure the destination log stream that metrics should be delivered to. This value will be passed from the library to the agent in the Embedded Metric payload. If a LogGroup is not provided, the default value will be derived by the agent (this will likely be the hostname).

Requirements:

- Name Length 1-512 characters
- The ':' (colon) and '\*' (asterisk) characters are not allowed. Pattern: [^:]\*

Example:

```java
# in process
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;

Configuration config = EnvironmentConfigurationProvider.getConfig();
config.setLogStreamName(LogStreamName))

# environment
AWS_EMF_LOG_STREAM_NAME="LogStreamName"
```

**EnvironmentOverride**: Short circuit auto-environment detection by explicitly defining how events should be sent. This is not supported through programmatic access.

Valid values include:

- Local: no decoration and sends over stdout
- Lambda: decorates logs with Lambda metadata and sends over stdout
- Agent: no decoration and sends over TCP
- EC2: decorates logs with EC2 metadata and sends over TCP
- ECS: decorates logs with ECS metadata and sends over TCP

Example:

```shell
AWS_EMF_ENVIRONMENT="Local"
```

**AgentEndpoint**: For agent-based platforms, you may optionally configure the endpoint to reach the agent on.

Example:

```java
// in process
import software.amazon.cloudwatchlogs.emf.config.Configuration;
import software.amazon.cloudwatchlogs.emf.config.EnvironmentConfigurationProvider;

Configuration config = EnvironmentConfigurationProvider.getConfig();
config.setAgentEndpoint("udp://127.0.0.1:1000");

// environment
AWS_EMF_AGENT_ENDPOINT="udp://127.0.0.1:1000"
```

## Examples

Check out the [examples](https://github.com/awslabs/aws-embedded-metrics-java/tree/master/examples) directory to get started.


## Development

### Building

[Gradle](https://gradle.org/) is used to build the project. Run this command to build the project:
```
./gradlew build

```



### Testing

We have 2 different types of tests:

1. Unit tests. The command to run these tests

	```sh
	./gradlew test
	```

1. Integration tests. These tests require Docker to run the CloudWatch Agent and valid AWS credentials. Tests can be run by:

	```sh
	export AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY_ID
	export AWS_SECRET_ACCESS_KEY=YOUR_ACCESS_KEY
	export AWS_REGION=us-west-2
	./gradlew integ
	```

	**NOTE**: You need to replace the access key id and access key with your own AWS credentials.

### Formatting

We use [Spotless plugin](https://github.com/diffplug/spotless/tree/master/plugin-gradle) for style-checking.
To auto fix code style, run
```
./gradlew :spotlessApply
```

## License

This project is licensed under the Apache-2.0 License.
