package io.joshuasalcedo.logging;

import io.joshuasalcedo.logging.LogLevel;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;
import java.util.Map;

/**
 * Collects metrics about logging performance and usage
 */
public class LoggingMetrics {
    private static final LoggingMetrics INSTANCE = new LoggingMetrics();
    
    private final Map<LogLevel, LongAdder> logCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> loggerCounts = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> handlerCounts = new ConcurrentHashMap<>();
    
    private final AtomicLong totalLogs = new AtomicLong(0);
    private final AtomicLong droppedLogs = new AtomicLong(0);
    private final AtomicLong processingTimeTotal = new AtomicLong(0);
    private final AtomicLong slowestLogTime = new AtomicLong(0);
    private final Instant startTime = Instant.now();
    
    private LoggingMetrics() {
        // Initialize counters for all log levels
        for (LogLevel level : LogLevel.values()) {
            logCounts.put(level, new LongAdder());
        }
    }
    
    public static LoggingMetrics getInstance() {
        return INSTANCE;
    }
    
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
    }
    
    public void recordDroppedLog() {
        droppedLogs.incrementAndGet();
    }
    
    public LoggingStats getStats() {
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
    
    public void reset() {
        totalLogs.set(0);
        droppedLogs.set(0);
        processingTimeTotal.set(0);
        slowestLogTime.set(0);
        
        logCounts.values().forEach(LongAdder::reset);
        loggerCounts.clear();
        handlerCounts.clear();
    }
    
    /**
     * Immutable snapshot of logging statistics
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