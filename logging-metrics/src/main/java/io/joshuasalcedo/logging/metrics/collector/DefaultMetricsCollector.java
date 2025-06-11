package io.joshuasalcedo.logging.metrics.collector;

import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;

/**
 * Default implementation of MetricsCollector with comprehensive performance tracking
 */
public class DefaultMetricsCollector implements MetricsCollector {
    private final Map<LogLevel, LongAdder> logCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> loggerCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> handlerCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> errorCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> queueSizes = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> methodCounts = new ConcurrentHashMap<>();
    
    private final AtomicLong totalLogs = new AtomicLong(0);
    private final AtomicLong droppedLogs = new AtomicLong(0);
    private final AtomicLong handlerFailures = new AtomicLong(0);
    private final AtomicLong processingTimeTotal = new AtomicLong(0);
    private final AtomicLong asyncLatencyTotal = new AtomicLong(0);
    private final AtomicLong asyncLogCount = new AtomicLong(0);
    private final AtomicLong slowestLogTime = new AtomicLong(0);
    private final AtomicLong slowestAsyncLatency = new AtomicLong(0);
    private final AtomicLong configChanges = new AtomicLong(0);
    private final AtomicLong memoryUsage = new AtomicLong(0);
    private final AtomicLong peakMemoryUsage = new AtomicLong(0);
    private final Instant startTime = Instant.now();
    
    public DefaultMetricsCollector() {
        // Initialize counters for all log levels
        for (LogLevel level : LogLevel.values()) {
            logCounts.put(level, new LongAdder());
        }
    }
    
    @Override
    public void recordLog(LogLevel level, String loggerName, String handlerName, long processingTimeNanos) {
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
        
        // Update memory usage estimation
        updateMemoryUsage();
    }
    
    @Override
    public void recordDroppedLog() {
        droppedLogs.incrementAndGet();
    }
    
    @Override
    public void recordError(String errorType, Throwable error) {
        errorCounts.computeIfAbsent(errorType, k -> new LongAdder()).increment();
        
        // Record specific exception types
        if (error != null) {
            String exceptionType = error.getClass().getSimpleName();
            errorCounts.computeIfAbsent("Exception_" + exceptionType, k -> new LongAdder()).increment();
        }
    }
    
    @Override
    public void recordQueueSize(String queueName, int size) {
        queueSizes.computeIfAbsent(queueName, k -> new AtomicLong()).set(size);
    }
    
    @Override
    public void recordAsyncLatency(long latencyNanos) {
        asyncLogCount.incrementAndGet();
        asyncLatencyTotal.addAndGet(latencyNanos);
        
        // Track slowest async latency
        long currentSlowest = slowestAsyncLatency.get();
        if (latencyNanos > currentSlowest) {
            slowestAsyncLatency.compareAndSet(currentSlowest, latencyNanos);
        }
    }
    
    @Override
    public void recordHandlerFailure(String handlerName, Throwable failure) {
        handlerFailures.incrementAndGet();
        recordError("HandlerFailure_" + handlerName, failure);
    }
    
    @Override
    public void recordConfigurationChange(String configKey, String oldValue, String newValue) {
        configChanges.incrementAndGet();
    }
    
    public void recordMethodCall(String methodName) {
        methodCounts.computeIfAbsent(methodName, k -> new LongAdder()).increment();
    }
    
    private void updateMemoryUsage() {
        Runtime runtime = Runtime.getRuntime();
        long currentMemory = runtime.totalMemory() - runtime.freeMemory();
        memoryUsage.set(currentMemory);
        
        // Track peak memory usage
        long currentPeak = peakMemoryUsage.get();
        if (currentMemory > currentPeak) {
            peakMemoryUsage.compareAndSet(currentPeak, currentMemory);
        }
    }
    
    @Override
    public LoggingMetrics.LoggingStats getStats() {
        Map<LogLevel, Long> levelCounts = new ConcurrentHashMap<>();
        logCounts.forEach((level, adder) -> levelCounts.put(level, adder.sum()));
        
        Map<String, Long> loggerStats = new ConcurrentHashMap<>();
        loggerCounts.forEach((logger, adder) -> loggerStats.put(logger, adder.sum()));
        
        Map<String, Long> handlerStats = new ConcurrentHashMap<>();
        handlerCounts.forEach((handler, adder) -> handlerStats.put(handler, adder.sum()));
        
        long total = totalLogs.get();
        long avgProcessingTimeNanos = total > 0 ? processingTimeTotal.get() / total : 0;
        
        return new LoggingMetrics.LoggingStats(
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
    
    @Override
    public void reset() {
        totalLogs.set(0);
        droppedLogs.set(0);
        handlerFailures.set(0);
        processingTimeTotal.set(0);
        asyncLatencyTotal.set(0);
        asyncLogCount.set(0);
        slowestLogTime.set(0);
        slowestAsyncLatency.set(0);
        configChanges.set(0);
        memoryUsage.set(0);
        peakMemoryUsage.set(0);
        
        logCounts.values().forEach(LongAdder::reset);
        loggerCounts.clear();
        handlerCounts.clear();
        errorCounts.clear();
        queueSizes.clear();
        methodCounts.clear();
    }
    
    // Extended metrics getters
    public Map<String, Long> getErrorCounts() {
        Map<String, Long> errors = new ConcurrentHashMap<>();
        errorCounts.forEach((error, adder) -> errors.put(error, adder.sum()));
        return errors;
    }
    
    public Map<String, Long> getQueueSizes() {
        Map<String, Long> sizes = new ConcurrentHashMap<>();
        queueSizes.forEach((queue, size) -> sizes.put(queue, size.get()));
        return sizes;
    }
    
    public Map<String, Long> getMethodCounts() {
        Map<String, Long> methods = new ConcurrentHashMap<>();
        methodCounts.forEach((method, adder) -> methods.put(method, adder.sum()));
        return methods;
    }
    
    public long getHandlerFailures() {
        return handlerFailures.get();
    }
    
    public long getAverageAsyncLatencyNanos() {
        long count = asyncLogCount.get();
        return count > 0 ? asyncLatencyTotal.get() / count : 0;
    }
    
    public long getSlowestAsyncLatencyNanos() {
        return slowestAsyncLatency.get();
    }
    
    public long getConfigurationChanges() {
        return configChanges.get();
    }
    
    public long getCurrentMemoryUsage() {
        return memoryUsage.get();
    }
    
    public long getPeakMemoryUsage() {
        return peakMemoryUsage.get();
    }
    
    public double getErrorRate() {
        long total = totalLogs.get();
        if (total == 0) return 0.0;
        
        long totalErrors = errorCounts.values().stream()
                .mapToLong(LongAdder::sum)
                .sum();
        
        return (double) totalErrors / total * 100.0;
    }
    
    public double getThroughputPerSecond() {
        long durationSeconds = java.time.Duration.between(startTime, Instant.now()).getSeconds();
        if (durationSeconds == 0) return 0.0;
        
        return (double) totalLogs.get() / durationSeconds;
    }
    
    /**
     * Get comprehensive metrics report as formatted string
     */
    public String getDetailedReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Comprehensive Logging Metrics Report ===\n\n");
        
        // Basic stats
        LoggingMetrics.LoggingStats stats = getStats();
        report.append("Basic Statistics:\n");
        report.append(String.format("  Total Logs: %,d\n", stats.getTotalLogs()));
        report.append(String.format("  Dropped Logs: %,d (%.2f%%)\n", stats.getDroppedLogs(), stats.getDroppedPercentage()));
        report.append(String.format("  Handler Failures: %,d\n", getHandlerFailures()));
        report.append(String.format("  Error Rate: %.2f%%\n", getErrorRate()));
        report.append(String.format("  Throughput: %.2f logs/sec\n\n", getThroughputPerSecond()));
        
        // Performance metrics
        report.append("Performance Metrics:\n");
        report.append(String.format("  Average Processing Time: %.2f μs\n", stats.getAvgProcessingTimeNanos() / 1000.0));
        report.append(String.format("  Slowest Log Time: %.2f ms\n", stats.getSlowestLogTimeNanos() / 1_000_000.0));
        report.append(String.format("  Average Async Latency: %.2f μs\n", getAverageAsyncLatencyNanos() / 1000.0));
        report.append(String.format("  Slowest Async Latency: %.2f ms\n\n", getSlowestAsyncLatencyNanos() / 1_000_000.0));
        
        // Memory usage
        report.append("Memory Usage:\n");
        report.append(String.format("  Current: %.2f MB\n", getCurrentMemoryUsage() / 1024.0 / 1024.0));
        report.append(String.format("  Peak: %.2f MB\n\n", getPeakMemoryUsage() / 1024.0 / 1024.0));
        
        // Log level breakdown
        report.append("Log Level Breakdown:\n");
        stats.getLogLevelCounts().entrySet().stream()
                .sorted(Map.Entry.<LogLevel, Long>comparingByValue().reversed())
                .forEach(entry -> report.append(String.format("  %s: %,d\n", entry.getKey(), entry.getValue())));
        
        // Top loggers
        report.append("\nTop Loggers:\n");
        stats.getLoggerCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> report.append(String.format("  %s: %,d\n", entry.getKey(), entry.getValue())));
        
        // Handler usage
        report.append("\nHandler Usage:\n");
        stats.getHandlerCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> report.append(String.format("  %s: %,d\n", entry.getKey(), entry.getValue())));
        
        // Error breakdown
        Map<String, Long> errors = getErrorCounts();
        if (!errors.isEmpty()) {
            report.append("\nError Breakdown:\n");
            errors.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> report.append(String.format("  %s: %,d\n", entry.getKey(), entry.getValue())));
        }
        
        // Queue sizes
        Map<String, Long> queues = getQueueSizes();
        if (!queues.isEmpty()) {
            report.append("\nQueue Status:\n");
            queues.forEach((queue, size) -> report.append(String.format("  %s: %,d\n", queue, size)));
        }
        
        report.append(String.format("\nReport generated at: %s\n", Instant.now()));
        report.append("=====================================");
        
        return report.toString();
    }
}