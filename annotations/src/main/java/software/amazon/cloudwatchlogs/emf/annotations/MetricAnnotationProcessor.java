package software.amazon.cloudwatchlogs.emf.annotations;

import java.lang.reflect.Method;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.Unit;

@Aspect
public class MetricAnnotationProcessor {
    /** private struct used to translate all annotations to be handled the same */
    @AllArgsConstructor
    @Builder // For testing
    @Getter
    protected static class AnnotationTranslator {
        private final String name;
        private final Boolean logSuccess;
        private final Class<? extends Throwable>[] logExceptions;
        private final Boolean flush;
        private final String logger;
        private final double value;
        private final String defaultName;
        private final Unit unit;

        public AnnotationTranslator(CountMetric annotation) {
            this.name = annotation.name();
            this.logSuccess = annotation.logSuccess();
            this.logExceptions = annotation.logExceptions();
            this.flush = annotation.flush();
            this.logger = annotation.logger();
            this.value = annotation.value();
            this.defaultName = "Count";
            this.unit = Unit.COUNT;
        }

        public AnnotationTranslator(ExecutionTimeMetric annotation, double time) {
            this.name = annotation.name();
            this.logSuccess = annotation.logSuccess();
            this.logExceptions = annotation.logExceptions();
            this.flush = annotation.flush();
            this.logger = annotation.logger();
            this.value = time;
            this.defaultName = "ExecutionTime";
            this.unit = Unit.MILLISECONDS;
        }
    }

    /**
     * Puts a metric with the method count based on the parameters provided in the annotation.
     *
     * @param point The point for the annotated method.
     * @return The result of the method call.
     * @throws Throwable if the method fails.
     */
    @Around(
            "execution(* *(..)) && @annotation(software.amazon.cloudwatchlogs.emf.annotations.CountMetric)")
    public Object aroundCountMetric(final ProceedingJoinPoint point) throws Throwable {

        // Execute the method and capture whether a throwable is thrown.
        Throwable throwable = null;
        try {
            return point.proceed();
        } catch (final Throwable t) {
            throwable = t;
            throw t;
        } finally {
            final Method method = ((MethodSignature) point.getSignature()).getMethod();
            final CountMetric countMetricAnnotation = method.getAnnotation(CountMetric.class);

            handle(throwable, method, new AnnotationTranslator(countMetricAnnotation));
        }
    }

    /**
     * Puts a metric with the method count based on the parameters provided in the annotation.
     *
     * @param point The point for the annotated method.
     * @return The result of the method call.
     * @throws Throwable if the method fails.
     */
    @Around(
            "execution(* *(..)) && @annotation(software.amazon.cloudwatchlogs.emf.annotations.CountMetrics)")
    public Object aroundCountMetrics(final ProceedingJoinPoint point) throws Throwable {

        // Execute the method and capture whether a throwable is thrown.
        Throwable throwable = null;
        try {
            return point.proceed();
        } catch (final Throwable t) {
            throwable = t;
            throw t;
        } finally {
            final Method method = ((MethodSignature) point.getSignature()).getMethod();
            final CountMetrics countMetricsAnnotation = method.getAnnotation(CountMetrics.class);

            for (CountMetric countMetricAnnotation : countMetricsAnnotation.value()) {
                handle(throwable, method, new AnnotationTranslator(countMetricAnnotation));
            }
        }
    }

    /**
     * Puts a metric with the method time based on the parameters provided in the annotation.
     *
     * @param point The point for the annotated method.
     * @return The result of the method call.
     * @throws Throwable if the method fails.
     */
    @Around(
            "execution(* *(..)) && @annotation(software.amazon.cloudwatchlogs.emf.annotations.ExecutionTimeMetric)")
    public Object aroundExecutionTimeMetric(final ProceedingJoinPoint point) throws Throwable {

        // Execute the method and capture whether a throwable is thrown.
        final double startTime = System.currentTimeMillis(); // capture the start time
        Throwable throwable = null;
        try {
            return point.proceed();
        } catch (final Throwable t) {
            throwable = t;
            throw t;
        } finally {
            final double time = System.currentTimeMillis() - startTime; // capture the total time
            final Method method = ((MethodSignature) point.getSignature()).getMethod();
            final ExecutionTimeMetric ExecutionTimeMetricAnnotation =
                    method.getAnnotation(ExecutionTimeMetric.class);

            handle(
                    throwable,
                    method,
                    new AnnotationTranslator(ExecutionTimeMetricAnnotation, time));
        }
    }

    /**
     * Puts a metric with the method time based on the parameters provided in the annotation.
     *
     * @param point The point for the annotated method.
     * @return The result of the method call.
     * @throws Throwable if the method fails.
     */
    @Around(
            "execution(* *(..)) && @annotation(software.amazon.cloudwatchlogs.emf.annotations.ExecutionTimeMetrics)")
    public Object aroundExecutionTimeMetrics(final ProceedingJoinPoint point) throws Throwable {

        // Execute the method and capture whether a throwable is thrown.
        final double startTime = System.currentTimeMillis(); // capture the start time
        Throwable throwable = null;
        try {
            return point.proceed();
        } catch (final Throwable t) {
            throwable = t;
            throw t;
        } finally {
            final double time = System.currentTimeMillis() - startTime; // capture the total time
            final Method method = ((MethodSignature) point.getSignature()).getMethod();
            final ExecutionTimeMetrics ExecutionTimeMetricsAnnotation =
                    method.getAnnotation(ExecutionTimeMetrics.class);

            for (ExecutionTimeMetric ExecutionTimeMetricAnnotation :
                    ExecutionTimeMetricsAnnotation.value()) {
                handle(
                        throwable,
                        method,
                        new AnnotationTranslator(ExecutionTimeMetricAnnotation, time));
            }
        }
    }

    /** Handles the logging of all metrics related to an annotation and method */
    protected static void handle(
            Throwable throwable, Method method, AnnotationTranslator translator) {
        if (shouldLog(throwable, translator)) {
            final String metricName = getName(method, translator);
            final double value = translator.getValue();

            MetricsLogger logger = MetricAnnotationMediator.getLogger(translator.getLogger());
            logger.putMetric(metricName, value, translator.getUnit());

            if (translator.getFlush()) {
                MetricAnnotationMediator.flushAll();
            }
        }
    }

    /**
     * @return true if this annotation's metric should be logged according to the rules defined in
     *     the annotation.
     */
    protected static boolean shouldLog(Throwable throwable, AnnotationTranslator translator) {
        boolean shouldLog = false;
        for (final Class<? extends Throwable> failureClass : translator.getLogExceptions()) {
            shouldLog |= failureClass.isInstance(throwable);
        }

        shouldLog |= throwable == null && translator.getLogSuccess();

        return shouldLog;
    }

    /** @return the name of the metric based on the annotation and method */
    protected static String getName(Method method, AnnotationTranslator translator) {
        return translator.getName().isEmpty()
                ? String.format(
                        "%s.%s.%s",
                        method.getDeclaringClass().getSimpleName(),
                        method.getName(),
                        translator.getDefaultName())
                : translator.getName();
    }
}
