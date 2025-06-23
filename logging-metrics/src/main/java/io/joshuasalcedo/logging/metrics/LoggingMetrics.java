package io.joshuasalcedo.logging.metrics;

import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.metrics.collector.DefaultMetricsCollector;
import io.joshuasalcedo.logging.metrics.collector.MetricsCollector;
import io.joshuasalcedo.logging.metrics.export.MetricsExporter;
import io.joshuasalcedo.logging.metrics.export.JsonMetricsExporter;
import io.joshuasalcedo.logging.metrics.export.CsvMetricsExporter;
import io.joshuasalcedo.logging.metrics.export.PrometheusMetricsExporter;
import io.joshuasalcedo.logging.metrics.export.HtmlMetricsExporter;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Enhanced logging metrics system with comprehensive tracking and export capabilities
 */
public class LoggingMetrics {
    private static final LoggingMetrics INSTANCE = new LoggingMetrics();
    
    private final DefaultMetricsCollector collector;
    private final Map<String, MetricsExporter> exporters = new ConcurrentHashMap<>();
    private final List<MetricsListener> listeners = new CopyOnWriteArrayList<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "LoggingMetrics-Scheduler");
        t.setDaemon(true);
        return t;
    });
    
    // Legacy compatibility fields
    private final Map<LogLevel, LongAdder> logCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> loggerCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> handlerCounts = new ConcurrentHashMap<>();
    
    private final AtomicLong totalLogs = new AtomicLong(0);
    private final AtomicLong droppedLogs = new AtomicLong(0);
    private final AtomicLong processingTimeTotal = new AtomicLong(0);
    private final AtomicLong slowestLogTime = new AtomicLong(0);
    private final Instant startTime = Instant.now();
    
    // Configuration
    private volatile boolean autoReportEnabled = false;
    private volatile int autoReportIntervalMinutes = 5;
    private volatile long metricsRetentionHours = 24;
    
    private LoggingMetrics() {
        // Initialize legacy counters for all log levels
        for (LogLevel level : LogLevel.values()) {
            logCounts.put(level, new LongAdder());
        }
        
        // Initialize new collector
        this.collector = new DefaultMetricsCollector();
        
        // Register default exporters
        registerExporter("json", new JsonMetricsExporter(collector));
        registerExporter("csv", new CsvMetricsExporter(collector));
        registerExporter("prometheus", new PrometheusMetricsExporter(collector));
        registerExporter("html", new HtmlMetricsExporter(collector));
        
        // Setup shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }
    
    public static LoggingMetrics getInstance() {
        return INSTANCE;
    }
    
    /**
     * Get the metrics collector for advanced usage
     */
    public MetricsCollector getCollector() {
        return collector;
    }
    
    /**
     * Record a log event (legacy method - delegates to new collector)
     */
    public void recordLog(LogLevel level, String loggerName, String handlerName, long processingTimeNanos) {
        // Update legacy counters for backward compatibility
        totalLogs.incrementAndGet();
        logCounts.get(level).increment();
        loggerCounts.computeIfAbsent(loggerName, k -> new LongAdder()).increment();
        handlerCounts.computeIfAbsent(handlerName, k -> new LongAdder()).increment();
        
        processingTimeTotal.addAndGet(processingTimeNanos);
        
        // Track slowest log processing time
        long currentSlowest = slowestLogTime.get();
        if (processingTimeNanos > currentSlowest) {
            slowestLogTime.compareAndSet(currentSlowest, processingTimeNanos);
        }
        
        // Delegate to new collector
        collector.recordLog(level, loggerName, handlerName, processingTimeNanos);
        
        // Notify listeners
        notifyListeners(level, loggerName, handlerName);
    }
    
    /**
     * Record a dropped log event
     */
    public void recordDroppedLog() {
        droppedLogs.incrementAndGet();
        collector.recordDroppedLog();
    }
    
    /**
     * Record an error during logging
     */
    public void recordError(String errorType, Throwable error) {
        collector.recordError(errorType, error);
    }
    
    /**
     * Record queue size for monitoring
     */
    public void recordQueueSize(String queueName, int size) {
        collector.recordQueueSize(queueName, size);
    }
    
    /**
     * Record async processing latency
     */
    public void recordAsyncLatency(long latencyNanos) {
        collector.recordAsyncLatency(latencyNanos);
    }
    
    
    /**
     * Get comprehensive logging statistics
     */
    public LoggingStats getStats() {
        return collector.getStats();
    }
    
    /**
     * Get legacy statistics (for backward compatibility)
     */
    public LoggingStats getLegacyStats() {
        Map<LogLevel, Long> levelCounts = new ConcurrentHashMap<>();
        logCounts.forEach((level, adder) -> levelCounts.put(level, adder.sum()));
        
        Map<String, Long> loggerStats = new ConcurrentHashMap<>();
        loggerCounts.forEach((logger, adder) -> loggerStats.put(logger, adder.sum()));
        
        Map<String, Long> handlerStats = new ConcurrentHashMap<>();
        handlerCounts.forEach((handler, adder) -> handlerStats.put(handler, adder.sum()));
        
        long total = totalLogs.get();
        long avgProcessingTimeNanos = total > 0 ? processingTimeTotal.get() / total : 0;
        
        return new LoggingStats(
            total,
            droppedLogs.get(),
            levelCounts,
            loggerStats,
            handlerStats,
            avgProcessingTimeNanos,
            slowestLogTime.get(),
            startTime,
            Instant.now()
        );
    }
    
    /**
     * Reset all metrics
     */
    public void reset() {
        // Reset legacy counters
        totalLogs.set(0);
        droppedLogs.set(0);
        processingTimeTotal.set(0);
        slowestLogTime.set(0);
        
        logCounts.values().forEach(LongAdder::reset);
        loggerCounts.clear();
        handlerCounts.clear();
        
        // Reset new collector
        collector.reset();
    }
    
    // === Export functionality ===
    
    /**
     * Register a metrics exporter
     */
    public void registerExporter(String name, MetricsExporter exporter) {
        exporters.put(name, exporter);
    }
    
    /**
     * Get an exporter by name
     */
    public MetricsExporter getExporter(String name) {
        return exporters.get(name);
    }
    
    /**
     * Export metrics in the specified format
     */
    public String exportMetrics(String format) {
        MetricsExporter exporter = exporters.get(format.toLowerCase());
        if (exporter == null) {
            throw new IllegalArgumentException("Unknown export format: " + format);
        }
        return exporter.export();
    }
    
    /**
     * Export metrics to output stream in the specified format
     */
    public void exportMetrics(String format, java.io.OutputStream outputStream) {
        MetricsExporter exporter = exporters.get(format.toLowerCase());
        if (exporter == null) {
            throw new IllegalArgumentException("Unknown export format: " + format);
        }
        exporter.export(outputStream);
    }
    
    /**
     * Get all available export formats
     */
    public java.util.Set<String> getAvailableFormats() {
        return exporters.keySet();
    }
    
    // === Event listeners ===
    
    /**
     * Add a metrics listener
     */
    public void addListener(MetricsListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a metrics listener
     */
    public void removeListener(MetricsListener listener) {
        listeners.remove(listener);
    }
    
    private void notifyListeners(LogLevel level, String loggerName, String handlerName) {
        for (MetricsListener listener : listeners) {
            try {
                listener.onLogEvent(level, loggerName, handlerName);
            } catch (Exception e) {
                // Don't let listener exceptions break metrics collection
                System.err.println("Error in metrics listener: " + e.getMessage());
            }
        }
    }
    
    // === Configuration ===
    
    /**
     * Enable automatic reporting
     */
    public void enableAutoReporting(int intervalMinutes) {
        this.autoReportIntervalMinutes = intervalMinutes;
        this.autoReportEnabled = true;
        
        scheduler.scheduleAtFixedRate(this::generateAutoReport, 
                intervalMinutes, intervalMinutes, TimeUnit.MINUTES);
    }
    
    /**
     * Disable automatic reporting
     */
    public void disableAutoReporting() {
        this.autoReportEnabled = false;
    }
    
    private void generateAutoReport() {
        if (!autoReportEnabled) return;
        
        try {
            String report = collector.getDetailedReport();
            System.out.println("=== Auto-Generated Metrics Report ===");
            System.out.println(report);
        } catch (Exception e) {
            System.err.println("Error generating auto report: " + e.getMessage());
        }
    }
    
    /**
     * Get detailed metrics report
     */
    public String getDetailedReport() {
        return collector.getDetailedReport();
    }
    
    /**
     * Shutdown the metrics system
     */
    public void shutdown() {
        autoReportEnabled = false;
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
    
    // === Utility methods ===
    
    /**
     * Check if metrics collection is healthy
     */
    public boolean isHealthy() {
        try {
            LoggingStats stats = getStats();
            
            // Basic health checks
            if (collector.getErrorRate() > 50.0) return false; // More than 50% errors
            if (collector.getHandlerFailures() > stats.getTotalLogs() * 0.1) return false; // More than 10% handler failures
            
            // Performance checks
            if (stats.getAvgProcessingTimeNanos() > 10_000_000) return false; // More than 10ms average
            
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get performance summary
     */
    public String getPerformanceSummary() {
        LoggingStats stats = getStats();
        return String.format(
            "Performance Summary: %.2f logs/sec, %.2fμs avg, %.2f%% errors, %s",
            collector.getThroughputPerSecond(),
            stats.getAvgProcessingTimeNanos() / 1000.0,
            collector.getErrorRate(),
            isHealthy() ? "HEALTHY" : "UNHEALTHY"
        );
    }
    
    /**
     * Interface for metrics event listeners
     */
    public interface MetricsListener {
        void onLogEvent(LogLevel level, String loggerName, String handlerName);
    }
    
    /**
     * Enhanced immutable snapshot of logging statistics
     */
    public static class LoggingStats {
        private final long totalLogs;
        private final long droppedLogs;
        private final Map<LogLevel, Long> logLevelCounts;
        private final Map<String, Long> loggerCounts;
        private final Map<String, Long> handlerCounts;
        private final long avgProcessingTimeNanos;
        private final long slowestLogTimeNanos;
        private final Instant startTime;
        private final Instant endTime;
        
        public LoggingStats(long totalLogs, long droppedLogs, 
                           Map<LogLevel, Long> logLevelCounts,
                           Map<String, Long> loggerCounts,
                           Map<String, Long> handlerCounts,
                           long avgProcessingTimeNanos,
                           long slowestLogTimeNanos,
                           Instant startTime, Instant endTime) {
            this.totalLogs = totalLogs;
            this.droppedLogs = droppedLogs;
            this.logLevelCounts = Map.copyOf(logLevelCounts);
            this.loggerCounts = Map.copyOf(loggerCounts);
            this.handlerCounts = Map.copyOf(handlerCounts);
            this.avgProcessingTimeNanos = avgProcessingTimeNanos;
            this.slowestLogTimeNanos = slowestLogTimeNanos;
            this.startTime = startTime;
            this.endTime = endTime;
        }
        
        // Getters
        public long getTotalLogs() { return totalLogs; }
        public long getDroppedLogs() { return droppedLogs; }
        public double getDroppedPercentage() { 
            return totalLogs > 0 ? (double) droppedLogs / totalLogs * 100 : 0; 
        }
        public Map<LogLevel, Long> getLogLevelCounts() { return logLevelCounts; }
        public Map<String, Long> getLoggerCounts() { return loggerCounts; }
        public Map<String, Long> getHandlerCounts() { return handlerCounts; }
        public long getAvgProcessingTimeNanos() { return avgProcessingTimeNanos; }
        public long getSlowestLogTimeNanos() { return slowestLogTimeNanos; }
        public double getLogsPerSecond() {
            long durationSeconds = java.time.Duration.between(startTime, endTime).getSeconds();
            return durationSeconds > 0 ? (double) totalLogs / durationSeconds : 0;
        }
        
        public Instant getStartTime() { return startTime; }
        public Instant getEndTime() { return endTime; }
        
        public java.time.Duration getDuration() {
            return java.time.Duration.between(startTime, endTime);
        }
        
        public String getFormattedDuration() {
            java.time.Duration duration = getDuration();
            long hours = duration.toHours();
            long minutes = duration.toMinutesPart();
            long seconds = duration.toSecondsPart();
            
            if (hours > 0) {
                return String.format("%dh %dm %ds", hours, minutes, seconds);
            } else if (minutes > 0) {
                return String.format("%dm %ds", minutes, seconds);
            } else {
                return String.format("%ds", seconds);
            }
        }
        
        @Override
        public String toString() {
            return String.format(
                "LoggingStats{total=%d, dropped=%d (%.2f%%), avgTime=%.2fμs, logsPerSec=%.2f}",
                totalLogs, droppedLogs, getDroppedPercentage(), 
                avgProcessingTimeNanos / 1000.0, getLogsPerSecond()
            );
        }
    }
}