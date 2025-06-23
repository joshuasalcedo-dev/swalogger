package io.joshuasalcedo.logging.metrics.collector;

import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.metrics.LoggingMetrics;

/**
 * Interface for collecting logging metrics
 */
public interface MetricsCollector {
    
    /**
     * Record a log event with performance metrics
     */
    void recordLog(LogLevel level, String loggerName, String handlerName, long processingTimeNanos);
    
    /**
     * Record a dropped log event (when queues are full, etc.)
     */
    void recordDroppedLog();
    
    /**
     * Record an error or exception during logging
     */
    void recordError(String errorType, Throwable error);
    
    /**
     * Record current queue size for async logging
     */
    void recordQueueSize(String queueName, int size);
    
    /**
     * Record latency for async log processing
     */
    void recordAsyncLatency(long latencyNanos);
    
    /**
     * Record handler failure
     */
    void recordHandlerFailure(String handlerName, Throwable failure);
    
    /**
     * Record configuration changes
     */
    void recordConfigurationChange(String configKey, String oldValue, String newValue);
    
    /**
     * Get current statistics snapshot
     */
    LoggingMetrics.LoggingStats getStats();
    
    /**
     * Reset all metrics counters
     */
    void reset();
}
