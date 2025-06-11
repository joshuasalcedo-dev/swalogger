package io.joshuasalcedo.logging.spring.actuator;

import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import io.joshuasalcedo.logging.metrics.collector.DefaultMetricsCollector;
import io.joshuasalcedo.logging.manager.LoggerManager;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for Joshua Salcedo Logging Framework
 */
@Component("loggingHealthIndicator")
public class LoggingHealthIndicator implements HealthIndicator {
    
    private final LoggingMetrics metrics;
    
    // Thresholds for health status
    private static final double ERROR_RATE_THRESHOLD = 5.0; // 5% error rate
    private static final double DROPPED_LOGS_THRESHOLD = 1.0; // 1% dropped logs
    private static final long HANDLER_FAILURE_THRESHOLD = 100; // 100 handler failures
    
    public LoggingHealthIndicator(LoggingMetrics metrics) {
        this.metrics = metrics;
    }
    
    @Override
    public Health health() {
        try {
            // Get current metrics
            LoggingMetrics.LoggingStats stats = metrics.getStats();
            DefaultMetricsCollector collector = (DefaultMetricsCollector) metrics.getCollector();
            double errorRate = collector.getErrorRate();
            long handlerFailures = collector.getHandlerFailures();
            double droppedPercentage = stats.getDroppedPercentage();
            
            // Build health details
            Map<String, Object> details = new HashMap<>();
            details.put("totalLogs", stats.getTotalLogs());
            details.put("droppedLogs", stats.getDroppedLogs());
            details.put("droppedPercentage", String.format("%.2f%%", droppedPercentage));
            details.put("errorRate", String.format("%.2f%%", errorRate));
            details.put("handlerFailures", handlerFailures);
            details.put("handlers", LoggerManager.getRootHandlers().size());
            details.put("activeLoggers", stats.getLoggerCounts().size());
            details.put("throughput", String.format("%.2f logs/sec", collector.getThroughputPerSecond()));
            
            // Add performance metrics
            details.put("avgProcessingTime", String.format("%.2f μs", stats.getAvgProcessingTimeNanos() / 1000.0));
            details.put("slowestLogTime", String.format("%.2f ms", stats.getSlowestLogTimeNanos() / 1_000_000.0));
            
            // Add memory metrics
            long currentMemory = collector.getCurrentMemoryUsage();
            long peakMemory = collector.getPeakMemoryUsage();
            details.put("memoryUsage", formatBytes(currentMemory));
            details.put("peakMemoryUsage", formatBytes(peakMemory));
            
            // Determine health status
            Health.Builder builder;
            
            if (errorRate > ERROR_RATE_THRESHOLD || 
                droppedPercentage > DROPPED_LOGS_THRESHOLD || 
                handlerFailures > HANDLER_FAILURE_THRESHOLD) {
                
                builder = Health.down();
                
                // Add specific problem indicators
                if (errorRate > ERROR_RATE_THRESHOLD) {
                    builder.withDetail("issue", "High error rate detected");
                }
                if (droppedPercentage > DROPPED_LOGS_THRESHOLD) {
                    builder.withDetail("warning", "Logs are being dropped");
                }
                if (handlerFailures > HANDLER_FAILURE_THRESHOLD) {
                    builder.withDetail("alert", "Handler failures detected");
                }
            } else if (errorRate > ERROR_RATE_THRESHOLD / 2 || 
                       droppedPercentage > DROPPED_LOGS_THRESHOLD / 2) {
                // Warning state
                builder = Health.up()
                    .withDetail("status", "degraded")
                    .withDetail("message", "Performance degradation detected");
            } else {
                // Healthy state
                builder = Health.up()
                    .withDetail("status", "healthy");
            }
            
            // Add all details
            for (Map.Entry<String, Object> entry : details.entrySet()) {
                builder.withDetail(entry.getKey(), entry.getValue());
            }
            
            return builder.build();
            
        } catch (Exception e) {
            return Health.down()
                .withException(e)
                .withDetail("error", "Failed to retrieve logging metrics")
                .build();
        }
    }
    
    /**
     * Format bytes into human-readable format
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}