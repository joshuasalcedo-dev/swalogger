package io.joshuasalcedo.logging.core;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.handler.Handler;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Logger {
    private final String name;
    private LogLevel level = LogLevel.INFO;
    private final List<Handler> handlers = new CopyOnWriteArrayList<>();
    private Logger parent;
    private boolean useParentHandlers = true;
    private boolean metricsEnabled = true;
    private static volatile boolean globalMetricsEnabled = true;
    
    public Logger(String name) {
        this.name = name;
    }
    
    public void log(LogLevel level, String message) {
        if (level.getValue() < this.level.getValue()) {
            return;
        }
        
        Log record = new Log(level, message, name);
        publish(record);
    }
    
    public void log(LogLevel level, String message, Throwable throwable) {
        if (level.getValue() < this.level.getValue()) {
            return;
        }
        
        Log record = new Log(level, message, name, throwable);
        publish(record);
    }
    
    private void publish(Log record) {
        long startTime = System.nanoTime();
        
        try {
            // Publish to this logger's handlers
            for (Handler handler : handlers) {
                long handlerStartTime = System.nanoTime();
                try {
                    handler.publish(record);
                    
                    // Record metrics for successful handler execution
                    if (shouldRecordMetrics()) {
                        long processingTime = System.nanoTime() - handlerStartTime;
                        recordMetrics(record.getLevel(), this.name, handler.getClass().getSimpleName(), processingTime);
                    }
                } catch (Exception e) {
                    // Record handler failure metrics
                    if (shouldRecordMetrics()) {
                        recordHandlerFailure(handler.getClass().getSimpleName(), e);
                    }
                    // Re-throw to maintain existing error handling behavior
                    throw e;
                }
            }
            
            // Publish to parent handlers if enabled
            if (useParentHandlers && parent != null) {
                parent.publish(record);
            }
            
        } catch (Exception e) {
            // Record general logging error
            if (shouldRecordMetrics()) {
                recordLoggingError("PublishError", e);
            }
            // Don't break logging due to metrics issues
        }
    }
    
    private boolean shouldRecordMetrics() {
        return globalMetricsEnabled && metricsEnabled;
    }
    
    private void recordMetrics(LogLevel level, String loggerName, String handlerName, long processingTimeNanos) {
        try {
            // Use reflection to avoid hard dependency on metrics module
            Class<?> metricsClass = Class.forName("io.joshuasalcedo.logging.metrics.LoggingMetrics");
            Object metricsInstance = metricsClass.getMethod("getInstance").invoke(null);
            metricsClass.getMethod("recordLog", LogLevel.class, String.class, String.class, long.class)
                       .invoke(metricsInstance, level, loggerName, handlerName, processingTimeNanos);
        } catch (Exception e) {
            // Silently ignore metrics errors to not break logging
        }
    }
    
    private void recordHandlerFailure(String handlerName, Throwable failure) {
        try {
            Class<?> metricsClass = Class.forName("io.joshuasalcedo.logging.metrics.LoggingMetrics");
            Object metricsInstance = metricsClass.getMethod("getInstance").invoke(null);
            Object collector = metricsClass.getMethod("getCollector").invoke(metricsInstance);
            collector.getClass().getMethod("recordHandlerFailure", String.class, Throwable.class)
                    .invoke(collector, handlerName, failure);
        } catch (Exception e) {
            // Silently ignore metrics errors
        }
    }
    
    private void recordLoggingError(String errorType, Throwable error) {
        try {
            Class<?> metricsClass = Class.forName("io.joshuasalcedo.logging.metrics.LoggingMetrics");
            Object metricsInstance = metricsClass.getMethod("getInstance").invoke(null);
            Object collector = metricsClass.getMethod("getCollector").invoke(metricsInstance);
            collector.getClass().getMethod("recordError", String.class, Throwable.class)
                    .invoke(collector, errorType, error);
        } catch (Exception e) {
            // Silently ignore metrics errors
        }
    }
    
    // Convenience methods
    public void debug(String message) {
        recordMethodCall("debug");
        log(LogLevel.DEBUG, message);
    }
    
    public void info(String message) {
        recordMethodCall("info");
        log(LogLevel.INFO, message);
    }
    
    public void warning(String message) {
        recordMethodCall("warning");
        log(LogLevel.WARN, message);
    }
    
    public void error(String message) {
        recordMethodCall("error");
        log(LogLevel.ERROR, message);
    }
    
    public void error(String message, Throwable throwable) {
        recordMethodCall("error");
        log(LogLevel.ERROR, message, throwable);
    }
    
    public void critical(String message) {
        recordMethodCall("critical");
        log(LogLevel.CRITICAL, message);
    }
    
    // Handler management
    public void addHandler(Handler handler) {
        handlers.add(handler);
    }
    
    public void removeHandler(Handler handler) {
        handlers.remove(handler);
    }
    
    public List<Handler> getHandlers() {
        return new ArrayList<>(handlers);
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setLevel(LogLevel level) {
        this.level = level;
    }
    
    public LogLevel getLevel() {
        return level;
    }
    
    public void setParent(Logger parent) {
        this.parent = parent;
    }
    
    public Logger getParent() {
        return parent;
    }
    
    public void setUseParentHandlers(boolean useParentHandlers) {
        this.useParentHandlers = useParentHandlers;
    }
    
    // === Metrics control ===
    
    /**
     * Enable or disable metrics collection for this logger
     */
    public void setMetricsEnabled(boolean enabled) {
        this.metricsEnabled = enabled;
    }
    
    /**
     * Check if metrics are enabled for this logger
     */
    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }
    
    /**
     * Enable or disable metrics collection globally for all loggers
     */
    public static void setGlobalMetricsEnabled(boolean enabled) {
        globalMetricsEnabled = enabled;
    }
    
    /**
     * Check if metrics are enabled globally
     */
    public static boolean isGlobalMetricsEnabled() {
        return globalMetricsEnabled;
    }
    
    /**
     * Get performance metrics for this logger
     */
    public String getMetricsSummary() {
        try {
            Class<?> metricsClass = Class.forName("io.joshuasalcedo.logging.metrics.LoggingMetrics");
            Object metricsInstance = metricsClass.getMethod("getInstance").invoke(null);
            Object stats = metricsClass.getMethod("getStats").invoke(metricsInstance);
            
            // Get logger-specific stats
            Object loggerCounts = stats.getClass().getMethod("getLoggerCounts").invoke(stats);
            Object loggerCount = ((Map<?, ?>) loggerCounts).get(this.name);
            
            if (loggerCount != null) {
                return String.format("Logger '%s': %s logs", this.name, loggerCount);
            } else {
                return String.format("Logger '%s': No activity recorded", this.name);
            }
        } catch (Exception e) {
            return "Metrics not available: " + e.getMessage();
        }
    }
    
    /**
     * Method call tracking for metrics
     */
    private void recordMethodCall(String methodName) {
        if (!shouldRecordMetrics()) return;
        
        try {
            Class<?> metricsClass = Class.forName("io.joshuasalcedo.logging.metrics.LoggingMetrics");
            Object metricsInstance = metricsClass.getMethod("getInstance").invoke(null);
            Object collector = metricsClass.getMethod("getCollector").invoke(metricsInstance);
            
            // Check if the collector has recordMethodCall method
            try {
                collector.getClass().getMethod("recordMethodCall", String.class)
                        .invoke(collector, this.name + "." + methodName);
            } catch (NoSuchMethodException e) {
                // Method doesn't exist in collector, which is fine
            }
        } catch (Exception e) {
            // Silently ignore metrics errors
        }
    }
    
}