package io.joshuasalcedo.logging.spring.actuator;

import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import io.joshuasalcedo.logging.metrics.collector.DefaultMetricsCollector;
import io.joshuasalcedo.logging.metrics.export.*;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.Selector;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Actuator endpoint for logging metrics
 */
@Component
@Endpoint(id = "loggingMetrics")
public class LoggingMetricsEndpoint {
    
    private final LoggingMetrics metrics;
    
    public LoggingMetricsEndpoint(LoggingMetrics metrics) {
        this.metrics = metrics;
    }
    
    /**
     * Get current logging metrics
     */
    @ReadOperation
    public Map<String, Object> metrics() {
        LoggingMetrics.LoggingStats stats = metrics.getStats();
        Map<String, Object> result = new HashMap<>();
        
        // Basic statistics
        Map<String, Object> basicStats = new HashMap<>();
        basicStats.put("totalLogs", stats.getTotalLogs());
        basicStats.put("droppedLogs", stats.getDroppedLogs());
        basicStats.put("droppedPercentage", stats.getDroppedPercentage());
        DefaultMetricsCollector collector = (DefaultMetricsCollector) metrics.getCollector();
        basicStats.put("throughput", collector.getThroughputPerSecond());
        basicStats.put("errorRate", collector.getErrorRate());
        basicStats.put("handlerFailures", collector.getHandlerFailures());
        result.put("basicStatistics", basicStats);
        
        // Performance metrics
        Map<String, Object> performance = new HashMap<>();
        performance.put("avgProcessingTimeNanos", stats.getAvgProcessingTimeNanos());
        performance.put("avgProcessingTimeMicros", stats.getAvgProcessingTimeNanos() / 1000.0);
        performance.put("slowestLogTimeNanos", stats.getSlowestLogTimeNanos());
        performance.put("slowestLogTimeMillis", stats.getSlowestLogTimeNanos() / 1_000_000.0);
        performance.put("avgAsyncLatencyNanos", collector.getAverageAsyncLatencyNanos());
        performance.put("slowestAsyncLatencyNanos", collector.getSlowestAsyncLatencyNanos());
        result.put("performance", performance);
        
        // Memory metrics
        Map<String, Object> memory = new HashMap<>();
        memory.put("currentUsageBytes", collector.getCurrentMemoryUsage());
        memory.put("currentUsageMB", collector.getCurrentMemoryUsage() / 1024.0 / 1024.0);
        memory.put("peakUsageBytes", collector.getPeakMemoryUsage());
        memory.put("peakUsageMB", collector.getPeakMemoryUsage() / 1024.0 / 1024.0);
        result.put("memory", memory);
        
        // Breakdowns
        result.put("logLevelCounts", convertLogLevelCounts(stats.getLogLevelCounts()));
        result.put("topLoggers", getTopItems(stats.getLoggerCounts(), 10));
        result.put("handlerCounts", stats.getHandlerCounts());
        result.put("errorCounts", getTopItems(collector.getErrorCounts(), 10));
        result.put("queueSizes", collector.getQueueSizes());
        
        // Health status
        result.put("isHealthy", metrics.isHealthy());
        
        return result;
    }
    
    /**
     * Export metrics in specified format
     */
    @ReadOperation(produces = {"application/json", "text/csv", "text/plain", "text/html"})
    public String export(@Selector String format) {
        DefaultMetricsCollector collector = (DefaultMetricsCollector) metrics.getCollector();
        MetricsExporter exporter;
        
        switch (format.toLowerCase()) {
            case "csv":
                exporter = new CsvMetricsExporter(collector);
                break;
            case "prometheus":
                exporter = new PrometheusMetricsExporter(collector);
                break;
            case "html":
                exporter = new HtmlMetricsExporter(collector);
                break;
            case "json":
            default:
                exporter = new JsonMetricsExporter(collector);
                break;
        }
        
        return exporter.export();
    }
    
    /**
     * Get performance summary
     */
    @ReadOperation
    public String summary() {
        return metrics.getPerformanceSummary();
    }
    
    /**
     * Get detailed report
     */
    @ReadOperation
    public String report() {
        DefaultMetricsCollector collector = (DefaultMetricsCollector) metrics.getCollector();
        return collector.getDetailedReport();
    }
    
    /**
     * Reset all metrics
     */
    @WriteOperation
    public Map<String, String> reset() {
        metrics.reset();
        Map<String, String> result = new HashMap<>();
        result.put("status", "success");
        result.put("message", "All metrics have been reset");
        result.put("timestamp", java.time.Instant.now().toString());
        return result;
    }
    
    /**
     * Convert log level counts to string keys
     */
    private Map<String, Long> convertLogLevelCounts(Map<io.joshuasalcedo.logging.core.LogLevel, Long> logLevelCounts) {
        Map<String, Long> result = new HashMap<>();
        logLevelCounts.forEach((level, count) -> result.put(level.toString(), count));
        return result;
    }
    
    /**
     * Get top N items from a map sorted by value
     */
    private Map<String, Long> getTopItems(Map<String, Long> items, int limit) {
        Map<String, Long> result = new HashMap<>();
        items.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }
}