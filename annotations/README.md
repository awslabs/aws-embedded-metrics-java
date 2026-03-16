# Annotations

## Setup

Getting this package to work requires the following additions to a gradle build file:
```gradle
plugins {
	...
	id "io.freefair.aspectj.post-compile-weaving" version "6.4.3"
}

dependencies {
	...
	implementation project(':annotations')
	implementation "org.aspectj:aspectjweaver:1.9.8.RC3"
}

```

## Usage

This pacakge adds `@CountMetric` and `@ExecutionTimeMetric` method annotations.

### @CountMetric
@CountMetric will log a value of 1 whenever it is triggered.

### @ExecutionTimeMetric
@ExecutionTimeMetric will log the execution time of a method whenever it is triggered.

### Annotation Parameters

- `String Name` (Default: `""`)
	- The name the metric is published under
	- If the name is left as `""` it will be replaced with `"<ClassName>.<MethodName>.<AnnotationType>"` when being sent to CWL
- `Boolean logSuccess` (Default `True` )
	- Determines if this annotation will log a metric when the method exits successfully
- `Class<? extends Throwable>[] LogErrors`  (Default: `[Throwable.class]` )
	- Determines if this annotation will log a metric when the method exits with an error (will log if the method exits by a throwing an in the given list)
- `String Logger` (Default: `""`)
	- Determines which logger this annotation’s metric will be put into.
	- If `null` or there is no logger associated with that key then the default logger will be used
- `bool flush` (Default: `false` )
	- Determines whether the metric logger attached to this annotation should flush after this function


### Loggers
This package also implements a singleton `MetricAnnotationMediator` which handles the logging of all metrics created by annotations. This singleton contains a default `MetricsLogger` instance that all annotations will default to; however, other loggers can be added to it using its `addLogger(name, logger)` method. Annotations can then be sent to the added logger by specifying the name of the logger as an annotation parameter.

There are two ways to flush these metrics to CloudWatch:
1. Call `MetricAnnotationMediator.flushAll()` which flushes all the loggers that the singleton has a reference to
2. Flush the logger that the metrics have been added to. Loggers can be retrieved from the `MetricAnnotationMediator` using the methods `MetricAnnotationMediator.getDefaultLogger()` and `MetricAnnotationMediator.getLogger(name)`

```java
import software.amazon.cloudwatchlogs.emf.annotations.CountMetric;
import software.amazon.cloudwatchlogs.emf.annotations.ExecutionTimeMetric;
import software.amazon.cloudwatchlogs.emf.annotations.MetricAnnotationMediator;

class Example {

	@CountMetric
	@ExecutionTimeMetric
	public static void doSomething() {
		// Do something
		...
	}

	public static void main(String[] args) {
		MetricsLogger metrics = new MetricsLogger();

		doSomething();

		MetricAnnotationMediator.flushAll();
	}
}
```
