package software.amazon.cloudwatchlogs.emf.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Allows multiple {@link CountMetric} annotations on a single method. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface CountMetrics {
    CountMetric[] value();
}
