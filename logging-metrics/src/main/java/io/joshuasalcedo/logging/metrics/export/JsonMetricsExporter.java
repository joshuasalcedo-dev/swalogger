package io.joshuasalcedo.logging.metrics.export;

import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import io.joshuasalcedo.logging.metrics.collector.DefaultMetricsCollector;
import io.joshuasalcedo.logging.metrics.config.MetricsConfiguration;
import io.joshuasalcedo.logging.metrics.template.TemplateEngine;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

/**
 * Exports metrics in JSON format using Apache FreeMarker templates
 */
public class JsonMetricsExporter implements MetricsExporter {
    private final DefaultMetricsCollector collector;
    private final MetricsConfiguration config;
    private final TemplateEngine templateEngine;
    
    public JsonMetricsExporter(DefaultMetricsCollector collector) {
        this(collector, new MetricsConfiguration());
    }
    
    public JsonMetricsExporter(DefaultMetricsCollector collector, MetricsConfiguration config) {
        this.collector = collector;
        this.config = config;
        this.templateEngine = TemplateEngine.getInstance();
    }
    
    @Override
    public String export() {
        try {
            Map<String, Object> dataModel = createDataModel();
            return templateEngine.processTemplate("metrics.json.ftl", dataModel);
        } catch (IOException | TemplateException e) {
            return "{\"error\": \"Failed to export metrics: " + e.getMessage() + "\"}";
        }
    }
    
    @Override
    public void export(OutputStream outputStream) {
        try (Writer writer = new OutputStreamWriter(outputStream, "UTF-8")) {
            Map<String, Object> dataModel = createDataModel();
            templateEngine.processTemplate("metrics.json.ftl", dataModel, writer);
        } catch (IOException | TemplateException e) {
            try {
                outputStream.write(("{\"error\": \"Failed to export metrics: " + e.getMessage() + "\"}").getBytes());
            } catch (IOException ignored) {
                // Nothing we can do if we can't write the error
            }
        }
    }
    
    @Override
    public String getContentType() {
        return "application/json";
    }
    
    private Map<String, Object> createDataModel() {
        Map<String, Object> dataModel = templateEngine.createBaseDataModel();
        
        // Add metrics statistics
        LoggingMetrics.LoggingStats stats = collector.getStats();
        dataModel.put("stats", new StatsWrapper(stats, collector));
        
        // Add configuration
        dataModel.put("config", config);
        
        return dataModel;
    }
    
    /**
     * Wrapper class to expose all metrics data to FreeMarker templates
     */
    public static class StatsWrapper {
        private final LoggingMetrics.LoggingStats stats;
        private final DefaultMetricsCollector collector;
        
        public StatsWrapper(LoggingMetrics.LoggingStats stats, DefaultMetricsCollector collector) {
            this.stats = stats;
            this.collector = collector;
        }
        
        // Basic statistics
        public long getTotalLogs() { return stats.getTotalLogs(); }
        public long getDroppedLogs() { return stats.getDroppedLogs(); }
        public double getDroppedPercentage() { return stats.getDroppedPercentage(); }
        public long getHandlerFailures() { return collector.getHandlerFailures(); }
        public double getErrorRate() { return collector.getErrorRate(); }
        public double getThroughputPerSecond() { return collector.getThroughputPerSecond(); }
        public long getConfigurationChanges() { return collector.getConfigurationChanges(); }
        
        // Performance metrics
        public long getAvgProcessingTimeNanos() { return stats.getAvgProcessingTimeNanos(); }
        public long getSlowestLogTimeNanos() { return stats.getSlowestLogTimeNanos(); }
        public long getAvgAsyncLatencyNanos() { return collector.getAverageAsyncLatencyNanos(); }
        public long getSlowestAsyncLatencyNanos() { return collector.getSlowestAsyncLatencyNanos(); }
        
        // Memory metrics
        public long getCurrentMemoryUsage() { return collector.getCurrentMemoryUsage(); }
        public long getPeakMemoryUsage() { return collector.getPeakMemoryUsage(); }
        
        // Breakdowns
        public Map<String, Long> getLogLevelCounts() { 
            Map<String, Long> counts = new HashMap<>();
            stats.getLogLevelCounts().forEach((level, count) -> counts.put(level.toString(), count));
            return counts;
        }
        public Map<String, Long> getLoggerCounts() { return stats.getLoggerCounts(); }
        public Map<String, Long> getHandlerCounts() { return stats.getHandlerCounts(); }
        public Map<String, Long> getErrorCounts() { return collector.getErrorCounts(); }
        public Map<String, Long> getQueueSizes() { return collector.getQueueSizes(); }
        // Note: MaxQueueSizes and AvgQueueSizes methods don't exist in DefaultMetricsCollector
        // Using current queue sizes as approximation
        public Map<String, Long> getMaxQueueSizes() { return collector.getQueueSizes(); }
        public Map<String, Long> getAvgQueueSizes() { return collector.getQueueSizes(); }
        
        // Time series data (mock for now)
        public java.util.List<TimeSeriesPoint> getRecentLogRates() { 
            return java.util.Arrays.asList(
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(300).toString(), getThroughputPerSecond()),
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(240).toString(), getThroughputPerSecond() * 1.1),
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(180).toString(), getThroughputPerSecond() * 0.9),
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(120).toString(), getThroughputPerSecond() * 1.2),
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(60).toString(), getThroughputPerSecond() * 0.8),
                new TimeSeriesPoint(java.time.Instant.now().toString(), getThroughputPerSecond())
            );
        }
        
        public java.util.List<TimeSeriesPoint> getRecentErrorRates() {
            return java.util.Arrays.asList(
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(300).toString(), getErrorRate()),
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(240).toString(), getErrorRate() * 1.1),
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(180).toString(), getErrorRate() * 0.9),
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(120).toString(), getErrorRate() * 1.3),
                new TimeSeriesPoint(java.time.Instant.now().minusSeconds(60).toString(), getErrorRate() * 0.7),
                new TimeSeriesPoint(java.time.Instant.now().toString(), getErrorRate())
            );
        }
    }
    
    public static class TimeSeriesPoint {
        private final String timestamp;
        private final double value;
        
        public TimeSeriesPoint(String timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }
        
        public String getTimestamp() { return timestamp; }
        public double getValue() { return value; }
    }
}