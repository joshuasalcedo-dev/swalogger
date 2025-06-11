package io.joshuasalcedo.logging.metrics.export;

import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import io.joshuasalcedo.logging.metrics.collector.DefaultMetricsCollector;
import io.joshuasalcedo.logging.core.LogLevel;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Exports metrics in CSV format for spreadsheet analysis
 */
public class CsvMetricsExporter implements MetricsExporter {
    private final DefaultMetricsCollector collector;
    private final boolean includeHeaders;
    
    public CsvMetricsExporter(DefaultMetricsCollector collector) {
        this(collector, true);
    }
    
    public CsvMetricsExporter(DefaultMetricsCollector collector, boolean includeHeaders) {
        this.collector = collector;
        this.includeHeaders = includeHeaders;
    }
    
    @Override
    public String export() {
        StringBuilder csv = new StringBuilder();
        LoggingMetrics.LoggingStats stats = collector.getStats();
        
        if (includeHeaders) {
            csv.append(createHeaderRow()).append("\\n");
        }
        
        csv.append(createSummaryRow(stats));
        
        // Add detailed breakdowns
        csv.append("\\n\\n# Log Level Breakdown\\n");
        csv.append("LogLevel,Count,Percentage\\n");
        long totalLogs = stats.getTotalLogs();
        for (Map.Entry<LogLevel, Long> entry : stats.getLogLevelCounts().entrySet()) {
            double percentage = totalLogs > 0 ? (double) entry.getValue() / totalLogs * 100 : 0;
            csv.append(String.format("%s,%d,%.2f%%\\n", 
                entry.getKey(), entry.getValue(), percentage));
        }
        
        // Logger breakdown
        csv.append("\\n# Logger Breakdown (Top 20)\\n");
        csv.append("LoggerName,Count,Percentage\\n");
        stats.getLoggerCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(20)
                .forEach(entry -> {
                    double percentage = totalLogs > 0 ? (double) entry.getValue() / totalLogs * 100 : 0;
                    csv.append(String.format("%s,%d,%.2f%%\\n", 
                        escapeCsvField(entry.getKey()), entry.getValue(), percentage));
                });
        
        // Handler breakdown
        csv.append("\\n# Handler Breakdown\\n");
        csv.append("HandlerName,Count,Percentage\\n");
        stats.getHandlerCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .forEach(entry -> {
                    double percentage = totalLogs > 0 ? (double) entry.getValue() / totalLogs * 100 : 0;
                    csv.append(String.format("%s,%d,%.2f%%\\n", 
                        entry.getKey(), entry.getValue(), percentage));
                });
        
        // Error breakdown
        Map<String, Long> errors = collector.getErrorCounts();
        if (!errors.isEmpty()) {
            csv.append("\\n# Error Breakdown\\n");
            csv.append("ErrorType,Count\\n");
            errors.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> csv.append(String.format("%s,%d\\n", 
                        escapeCsvField(entry.getKey()), entry.getValue())));
        }
        
        return csv.toString();
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
        return "text/csv";
    }
    
    private String createHeaderRow() {
        return "Timestamp,TotalLogs,DroppedLogs,DroppedPercentage,HandlerFailures,ErrorRate," +
               "ThroughputPerSec,AvgProcessingTimeMicros,SlowestLogTimeMs,AvgAsyncLatencyMicros," +
               "SlowestAsyncLatencyMs,CurrentMemoryMB,PeakMemoryMB,ConfigChanges," +
               "DebugLogs,InfoLogs,WarnLogs,ErrorLogs,CriticalLogs";
    }
    
    private String createSummaryRow(LoggingMetrics.LoggingStats stats) {
        StringBuilder row = new StringBuilder();
        
        // Basic metrics
        row.append(Instant.now().toString()).append(",");
        row.append(stats.getTotalLogs()).append(",");
        row.append(stats.getDroppedLogs()).append(",");
        row.append(String.format("%.2f", stats.getDroppedPercentage())).append(",");
        row.append(collector.getHandlerFailures()).append(",");
        row.append(String.format("%.2f", collector.getErrorRate())).append(",");
        row.append(String.format("%.2f", collector.getThroughputPerSecond())).append(",");
        
        // Performance metrics
        row.append(String.format("%.2f", stats.getAvgProcessingTimeNanos() / 1000.0)).append(",");
        row.append(String.format("%.2f", stats.getSlowestLogTimeNanos() / 1_000_000.0)).append(",");
        row.append(String.format("%.2f", collector.getAverageAsyncLatencyNanos() / 1000.0)).append(",");
        row.append(String.format("%.2f", collector.getSlowestAsyncLatencyNanos() / 1_000_000.0)).append(",");
        
        // Memory metrics
        row.append(String.format("%.2f", collector.getCurrentMemoryUsage() / 1024.0 / 1024.0)).append(",");
        row.append(String.format("%.2f", collector.getPeakMemoryUsage() / 1024.0 / 1024.0)).append(",");
        
        // Configuration changes
        row.append(collector.getConfigurationChanges()).append(",");
        
        // Log level counts
        Map<LogLevel, Long> levelCounts = stats.getLogLevelCounts();
        row.append(levelCounts.getOrDefault(LogLevel.DEBUG, 0L)).append(",");
        row.append(levelCounts.getOrDefault(LogLevel.INFO, 0L)).append(",");
        row.append(levelCounts.getOrDefault(LogLevel.WARN, 0L)).append(",");
        row.append(levelCounts.getOrDefault(LogLevel.ERROR, 0L)).append(",");
        row.append(levelCounts.getOrDefault(LogLevel.CRITICAL, 0L));
        
        return row.toString();
    }
    
    private String escapeCsvField(String field) {
        if (field == null) return "";
        
        // Escape quotes and wrap in quotes if contains comma, quote, or newline
        if (field.contains(",") || field.contains("\"") || field.contains("\\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    
    /**
     * Export time-series data for trending analysis
     */
    public String exportTimeSeries() {
        LoggingMetrics.LoggingStats stats = collector.getStats();
        StringBuilder csv = new StringBuilder();
        
        csv.append("Timestamp,TotalLogs,LogsPerSecond,AvgProcessingTime,MemoryUsage\\n");
        
        // For demonstration, we'll output current snapshot
        // In a real implementation, you'd collect these over time
        String timestamp = Instant.now().toString();
        csv.append(String.format("%s,%d,%.2f,%.2f,%.2f\\n",
                timestamp,
                stats.getTotalLogs(),
                collector.getThroughputPerSecond(),
                stats.getAvgProcessingTimeNanos() / 1000.0,
                collector.getCurrentMemoryUsage() / 1024.0 / 1024.0
        ));
        
        return csv.toString();
    }
    
    /**
     * Export logger performance comparison
     */
    public String exportLoggerComparison() {
        LoggingMetrics.LoggingStats stats = collector.getStats();
        StringBuilder csv = new StringBuilder();
        
        csv.append("LoggerName,LogCount,Percentage,Rank\\n");
        
        long totalLogs = stats.getTotalLogs();
        int rank = 1;
        
        for (Map.Entry<String, Long> entry : stats.getLoggerCounts().entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList())) {
            
            double percentage = totalLogs > 0 ? (double) entry.getValue() / totalLogs * 100 : 0;
            csv.append(String.format("%s,%d,%.2f%%,%d\\n",
                    escapeCsvField(entry.getKey()),
                    entry.getValue(),
                    percentage,
                    rank++
            ));
        }
        
        return csv.toString();
    }
}