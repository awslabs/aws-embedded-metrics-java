package software.amazon.cloudwatchlogs.emf.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to put a duration metric to CloudWatch Metrics. By default, when the
 * annotated method is called, the duration (in milliseconds) will be published with the metric name
 * "[ClassName].[methodName].Time". The metric name can be overridden, and the "applies" field can
 * be used to only publish for invocations when failures are/aren't thrown by the annotated method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(ExecutionTimeMetrics.class)
public @interface ExecutionTimeMetric {
    String name() default "";

    boolean logSuccess() default true;

    Class<? extends Throwable>[] logExceptions() default {Throwable.class};

    String logger() default "_defaultLogger";

    boolean flush() default false;
}
