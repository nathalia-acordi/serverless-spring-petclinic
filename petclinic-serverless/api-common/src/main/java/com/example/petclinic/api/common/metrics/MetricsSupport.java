package com.example.petclinic.api.common.metrics;

import software.amazon.lambda.powertools.metrics.Metrics;
import software.amazon.lambda.powertools.metrics.MetricsFactory;
import software.amazon.lambda.powertools.metrics.model.MetricUnit;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Metrics helper adapted for Powertools Java v2 API.
 * Emits individual EMF blobs per call (simple & explicit) with dimensions: Operation, Endpoint, Stage.
 * Stage comes from STAGE env var (defaults to dev).
 */
public final class MetricsSupport {

    private MetricsSupport() {}

    public static long startTimer() { return System.nanoTime(); }
    public static double endTimer(long startNano) { return (System.nanoTime() - startNano) / 1_000_000.0; }

    private static String stage() { return System.getenv().getOrDefault("STAGE", "dev"); }

    private static String sanitize(String v) {
        if (v == null || v.isBlank()) return "unknown";
        return v.replaceAll("\\s+", "_");
    }

    private static void withMetric(String operation, String endpoint, MetricEmitter emitter) {
        Metrics m = MetricsFactory.getMetricsInstance();
        m.addDimension("Operation", sanitize(operation));
        m.addDimension("Endpoint", sanitize(endpoint));
        m.addDimension("Stage", sanitize(stage()));
        emitter.emit(m);
        m.flush();
    }

    public static void increment(String counterName, String operation, String endpoint) {
        withMetric(operation, endpoint, m -> m.addMetric(counterName, 1, MetricUnit.COUNT));
    }

    public static void publishTimer(String metricName, double millis, String operation, String endpoint) {
        withMetric(operation, endpoint, m -> m.addMetric(metricName, millis, MetricUnit.MILLISECONDS));
    }

    public static <T> T time(String metricName, Supplier<T> supplier, String operation, String endpoint) {
        long s = startTimer();
        try { return supplier.get(); }
        finally { publishTimer(metricName, endTimer(s), operation, endpoint); }
    }

    public static <T> T time(String metricName, Callable<T> callable, String operation, String endpoint) {
        long s = startTimer();
        try { return callable.call(); }
        catch (RuntimeException re) { throw re; }
        catch (Exception e) { throw new RuntimeException(e); }
        finally { publishTimer(metricName, endTimer(s), operation, endpoint); }
    }

    public static void time(String metricName, Runnable runnable, String operation, String endpoint) {
        long s = startTimer();
        try { runnable.run(); }
        finally { publishTimer(metricName, endTimer(s), operation, endpoint); }
    }

    @FunctionalInterface
    private interface MetricEmitter { void emit(Metrics m); }
}
