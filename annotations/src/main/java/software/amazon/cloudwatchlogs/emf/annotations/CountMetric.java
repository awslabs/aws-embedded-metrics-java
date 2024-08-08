package software.amazon.cloudwatchlogs.emf.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation is used to put a count metric to CloudWatch Metrics. By default, when the
 * annotated method is called, the value 1.0 will be published with the metric name
 * "[ClassName].[methodName].Count". The value and metric name can be overridden, and the "applies"
 * field can be used to only publish for invocations when failures are/aren't thrown by the
 * annotated method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Repeatable(CountMetrics.class)
public @interface CountMetric {
    String name() default "";

    boolean logSuccess() default true;

    Class<? extends Throwable>[] logExceptions() default {Throwable.class};

    double value() default 1.0d;

    String logger() default "_defaultLogger";

    boolean flush() default false;
}
