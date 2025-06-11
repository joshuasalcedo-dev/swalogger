package io.joshuasalcedo.logging.metrics.export;

import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import io.joshuasalcedo.logging.metrics.collector.DefaultMetricsCollector;
import io.joshuasalcedo.logging.core.LogLevel;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Exports metrics in Prometheus format for monitoring systems
 */
public class PrometheusMetricsExporter implements MetricsExporter {
    private final DefaultMetricsCollector collector;
    private final String namespace;
    
    public PrometheusMetricsExporter(DefaultMetricsCollector collector) {
        this(collector, "logging");
    }
    
    public PrometheusMetricsExporter(DefaultMetricsCollector collector, String namespace) {
        this.collector = collector;
        this.namespace = sanitizeMetricName(namespace);
    }
    
    @Override
    public String export() {
        StringBuilder prometheus = new StringBuilder();
        LoggingMetrics.LoggingStats stats = collector.getStats();
        
        // Basic counters
        addCounter(prometheus, "logs_total", "Total number of logs processed", stats.getTotalLogs());
        addCounter(prometheus, "logs_dropped_total", "Total number of dropped logs", stats.getDroppedLogs());
        addCounter(prometheus, "handler_failures_total", "Total number of handler failures", collector.getHandlerFailures());
        addCounter(prometheus, "config_changes_total", "Total number of configuration changes", collector.getConfigurationChanges());
        
        // Gauges for current state
        addGauge(prometheus, "error_rate_percent", "Current error rate percentage", collector.getErrorRate());
        addGauge(prometheus, "throughput_logs_per_second", "Current throughput in logs per second", collector.getThroughputPerSecond());
        addGauge(prometheus, "memory_usage_bytes", "Current memory usage in bytes", collector.getCurrentMemoryUsage());
        addGauge(prometheus, "memory_peak_bytes", "Peak memory usage in bytes", collector.getPeakMemoryUsage());
        
        // Performance metrics
        addGauge(prometheus, "processing_time_avg_nanoseconds", "Average processing time in nanoseconds", stats.getAvgProcessingTimeNanos());
        addGauge(prometheus, "processing_time_slowest_nanoseconds", "Slowest processing time in nanoseconds", stats.getSlowestLogTimeNanos());
        addGauge(prometheus, "async_latency_avg_nanoseconds", "Average async latency in nanoseconds", collector.getAverageAsyncLatencyNanos());
        addGauge(prometheus, "async_latency_slowest_nanoseconds", "Slowest async latency in nanoseconds", collector.getSlowestAsyncLatencyNanos());
        
        // Log level breakdown
        for (Map.Entry<LogLevel, Long> entry : stats.getLogLevelCounts().entrySet()) {
            addCounterWithLabels(prometheus, "logs_by_level_total", 
                "Total logs by level", entry.getValue(), 
                "level", entry.getKey().toString().toLowerCase());
        }
        
        // Logger breakdown (top 50 to avoid metric explosion)
        stats.getLoggerCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(50)
                .forEach(entry -> {
                    String loggerName = sanitizeLabelValue(entry.getKey());
                    addCounterWithLabels(prometheus, "logs_by_logger_total", 
                        "Total logs by logger", entry.getValue(), 
                        "logger", loggerName);
                });
        
        // Handler breakdown
        for (Map.Entry<String, Long> entry : stats.getHandlerCounts().entrySet()) {
            addCounterWithLabels(prometheus, "logs_by_handler_total", 
                "Total logs by handler", entry.getValue(), 
                "handler", sanitizeLabelValue(entry.getKey()));
        }
        
        // Error breakdown
        for (Map.Entry<String, Long> entry : collector.getErrorCounts().entrySet()) {
            addCounterWithLabels(prometheus, "errors_by_type_total", 
                "Total errors by type", entry.getValue(), 
                "error_type", sanitizeLabelValue(entry.getKey()));
        }
        
        // Queue sizes
        for (Map.Entry<String, Long> entry : collector.getQueueSizes().entrySet()) {
            addGaugeWithLabels(prometheus, "queue_size", 
                "Current queue size", entry.getValue(), 
                "queue", sanitizeLabelValue(entry.getKey()));
        }
        
        // Method call counts (top 20)
        collector.getMethodCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .forEach(entry -> {
                    addCounterWithLabels(prometheus, "method_calls_total", 
                        "Total method calls", entry.getValue(), 
                        "method", sanitizeLabelValue(entry.getKey()));
                });
        
        return prometheus.toString();
    }
    
    @Override
    public void export(OutputStream outputStream) {
        try (PrintWriter writer = new PrintWriter(outputStream)) {
            writer.print(export());
            writer.flush();
        }
    }
    
    @Override
    public String getContentType() {
        return "text/plain; version=0.0.4";
    }
    
    private void addCounter(StringBuilder sb, String name, String help, long value) {
        String metricName = namespace + "_" + name;
        sb.append("# HELP ").append(metricName).append(" ").append(help).append("\\n");
        sb.append("# TYPE ").append(metricName).append(" counter\\n");
        sb.append(metricName).append(" ").append(value).append("\\n");
    }
    
    private void addGauge(StringBuilder sb, String name, String help, double value) {
        String metricName = namespace + "_" + name;
        sb.append("# HELP ").append(metricName).append(" ").append(help).append("\\n");
        sb.append("# TYPE ").append(metricName).append(" gauge\\n");
        sb.append(metricName).append(" ").append(value).append("\\n");
    }
    
    private void addCounterWithLabels(StringBuilder sb, String name, String help, long value, String labelName, String labelValue) {
        String metricName = namespace + "_" + name;
        
        // Only add help and type once per metric name
        if (!sb.toString().contains("# HELP " + metricName)) {
            sb.append("# HELP ").append(metricName).append(" ").append(help).append("\\n");
            sb.append("# TYPE ").append(metricName).append(" counter\\n");
        }
        
        sb.append(metricName).append("{").append(labelName).append("=\"").append(labelValue).append("\"} ").append(value).append("\\n");
    }
    
    private void addGaugeWithLabels(StringBuilder sb, String name, String help, long value, String labelName, String labelValue) {
        String metricName = namespace + "_" + name;
        
        // Only add help and type once per metric name
        if (!sb.toString().contains("# HELP " + metricName)) {
            sb.append("# HELP ").append(metricName).append(" ").append(help).append("\\n");
            sb.append("# TYPE ").append(metricName).append(" gauge\\n");
        }
        
        sb.append(metricName).append("{").append(labelName).append("=\"").append(labelValue).append("\"} ").append(value).append("\\n");
    }
    
    private String sanitizeMetricName(String name) {
        if (name == null) return "";
        // Prometheus metric names must match [a-zA-Z_:][a-zA-Z0-9_:]*
        return name.replaceAll("[^a-zA-Z0-9_:]", "_");
    }
    
    private String sanitizeLabelValue(String value) {
        if (value == null) return "";
        // Escape quotes and backslashes in label values
        return value.replace("\\\\", "\\\\\\\\").replace("\"", "\\\\\"").replace("\\n", "\\\\n");
    }
    
    /**
     * Export just basic metrics for lightweight monitoring
     */
    public String exportBasic() {
        StringBuilder prometheus = new StringBuilder();
        LoggingMetrics.LoggingStats stats = collector.getStats();
        
        // Just the essential metrics
        addCounter(prometheus, "logs_total", "Total number of logs processed", stats.getTotalLogs());
        addCounter(prometheus, "logs_dropped_total", "Total number of dropped logs", stats.getDroppedLogs());
        addGauge(prometheus, "error_rate_percent", "Current error rate percentage", collector.getErrorRate());
        addGauge(prometheus, "throughput_logs_per_second", "Current throughput in logs per second", collector.getThroughputPerSecond());
        addGauge(prometheus, "processing_time_avg_nanoseconds", "Average processing time in nanoseconds", stats.getAvgProcessingTimeNanos());
        
        // Log level counts
        for (Map.Entry<LogLevel, Long> entry : stats.getLogLevelCounts().entrySet()) {
            addCounterWithLabels(prometheus, "logs_by_level_total", 
                "Total logs by level", entry.getValue(), 
                "level", entry.getKey().toString().toLowerCase());
        }
        
        return prometheus.toString();
    }
}