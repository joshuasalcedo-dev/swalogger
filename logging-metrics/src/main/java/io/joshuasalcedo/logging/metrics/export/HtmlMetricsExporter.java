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

/**
 * Exports metrics in HTML format for web dashboard viewing
 */
public class HtmlMetricsExporter implements MetricsExporter {
    private final DefaultMetricsCollector collector;
    private final boolean includeCharts;
    
    public HtmlMetricsExporter(DefaultMetricsCollector collector) {
        this(collector, true);
    }
    
    public HtmlMetricsExporter(DefaultMetricsCollector collector, boolean includeCharts) {
        this.collector = collector;
        this.includeCharts = includeCharts;
    }
    
    @Override
    public String export() {
        LoggingMetrics.LoggingStats stats = collector.getStats();
        
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\\n");
        html.append("<html lang=\"en\">\\n");
        html.append("<head>\\n");
        html.append("    <meta charset=\"UTF-8\">\\n");
        html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\\n");
        html.append("    <title>Joshua Salcedo Logging Framework - Metrics Dashboard</title>\\n");
        html.append(getStyleSheet());
        html.append("</head>\\n");
        html.append("<body>\\n");
        
        // Header
        html.append("    <div class=\"container\">\\n");
        html.append("        <header>\\n");
        html.append("            <h1>📊 Logging Metrics Dashboard</h1>\\n");
        html.append("            <p class=\"subtitle\">Joshua Salcedo Logging Framework</p>\\n");
        html.append("            <p class=\"timestamp\">Generated: ").append(Instant.now().toString()).append("</p>\\n");
        html.append("        </header>\\n");
        
        // Summary cards
        html.append(createSummaryCards(stats));
        
        // Performance section
        html.append(createPerformanceSection(stats));
        
        // Breakdown sections
        html.append(createLogLevelSection(stats));
        html.append(createLoggerSection(stats));
        html.append(createHandlerSection(stats));
        html.append(createErrorSection());
        html.append(createQueueSection());
        
        // Memory and system info
        html.append(createSystemSection());
        
        if (includeCharts) {
            html.append(getChartScripts());
        }
        
        html.append("    </div>\\n");
        html.append("</body>\\n");
        html.append("</html>");
        
        return html.toString();
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
        return "text/html";
    }
    
    private String getStyleSheet() {
        return """
            <style>
                * {
                    margin: 0;
                    padding: 0;
                    box-sizing: border-box;
                }
                
                body {
                    font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                    min-height: 100vh;
                    color: #333;
                }
                
                .container {
                    max-width: 1200px;
                    margin: 0 auto;
                    padding: 20px;
                }
                
                header {
                    text-align: center;
                    background: rgba(255, 255, 255, 0.95);
                    border-radius: 15px;
                    padding: 30px;
                    margin-bottom: 30px;
                    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.1);
                }
                
                h1 {
                    color: #2c3e50;
                    font-size: 2.5em;
                    margin-bottom: 10px;
                }
                
                .subtitle {
                    color: #7f8c8d;
                    font-size: 1.2em;
                    margin-bottom: 15px;
                }
                
                .timestamp {
                    color: #95a5a6;
                    font-size: 0.9em;
                }
                
                .cards {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
                    gap: 20px;
                    margin-bottom: 30px;
                }
                
                .card {
                    background: rgba(255, 255, 255, 0.95);
                    border-radius: 10px;
                    padding: 25px;
                    text-align: center;
                    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
                    transition: transform 0.3s ease;
                }
                
                .card:hover {
                    transform: translateY(-5px);
                }
                
                .card-value {
                    font-size: 2.5em;
                    font-weight: bold;
                    color: #2c3e50;
                    margin-bottom: 5px;
                }
                
                .card-label {
                    color: #7f8c8d;
                    text-transform: uppercase;
                    font-size: 0.9em;
                    letter-spacing: 1px;
                }
                
                .section {
                    background: rgba(255, 255, 255, 0.95);
                    border-radius: 10px;
                    padding: 25px;
                    margin-bottom: 20px;
                    box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
                }
                
                .section h2 {
                    color: #2c3e50;
                    margin-bottom: 20px;
                    font-size: 1.5em;
                    border-bottom: 2px solid #ecf0f1;
                    padding-bottom: 10px;
                }
                
                .metric-row {
                    display: flex;
                    justify-content: space-between;
                    align-items: center;
                    padding: 10px 0;
                    border-bottom: 1px solid #ecf0f1;
                }
                
                .metric-row:last-child {
                    border-bottom: none;
                }
                
                .metric-name {
                    font-weight: 500;
                    color: #34495e;
                }
                
                .metric-value {
                    font-weight: bold;
                    color: #2c3e50;
                }
                
                .progress-bar {
                    width: 100%;
                    height: 20px;
                    background-color: #ecf0f1;
                    border-radius: 10px;
                    overflow: hidden;
                    margin: 10px 0;
                }
                
                .progress-fill {
                    height: 100%;
                    background: linear-gradient(90deg, #3498db, #2ecc71);
                    transition: width 0.3s ease;
                }
                
                .error { color: #e74c3c; }
                .warning { color: #f39c12; }
                .success { color: #27ae60; }
                .info { color: #3498db; }
                
                .grid {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                    gap: 20px;
                }
                
                .chart-container {
                    height: 300px;
                    margin: 20px 0;
                }
                
                @media (max-width: 768px) {
                    .container {
                        padding: 10px;
                    }
                    
                    .cards {
                        grid-template-columns: 1fr;
                    }
                    
                    h1 {
                        font-size: 2em;
                    }
                }
            </style>
            """;
    }
    
    private String createSummaryCards(LoggingMetrics.LoggingStats stats) {
        StringBuilder cards = new StringBuilder();
        cards.append("        <div class=\"cards\">\\n");
        
        // Total Logs
        cards.append("            <div class=\"card\">\\n");
        cards.append("                <div class=\"card-value\">").append(formatNumber(stats.getTotalLogs())).append("</div>\\n");
        cards.append("                <div class=\"card-label\">Total Logs</div>\\n");
        cards.append("            </div>\\n");
        
        // Throughput
        cards.append("            <div class=\"card\">\\n");
        cards.append("                <div class=\"card-value\">").append(String.format("%.1f", collector.getThroughputPerSecond())).append("</div>\\n");
        cards.append("                <div class=\"card-label\">Logs/Second</div>\\n");
        cards.append("            </div>\\n");
        
        // Error Rate
        cards.append("            <div class=\"card\">\\n");
        String errorClass = collector.getErrorRate() > 5 ? "error" : collector.getErrorRate() > 1 ? "warning" : "success";
        cards.append("                <div class=\"card-value ").append(errorClass).append("\">").append(String.format("%.2f%%", collector.getErrorRate())).append("</div>\\n");
        cards.append("                <div class=\"card-label\">Error Rate</div>\\n");
        cards.append("            </div>\\n");
        
        // Avg Processing Time
        cards.append("            <div class=\"card\">\\n");
        cards.append("                <div class=\"card-value\">").append(String.format("%.1f", stats.getAvgProcessingTimeNanos() / 1000.0)).append("μs</div>\\n");
        cards.append("                <div class=\"card-label\">Avg Processing</div>\\n");
        cards.append("            </div>\\n");
        
        cards.append("        </div>\\n");
        return cards.toString();
    }
    
    private String createPerformanceSection(LoggingMetrics.LoggingStats stats) {
        StringBuilder section = new StringBuilder();
        section.append("        <div class=\"section\">\\n");
        section.append("            <h2>⚡ Performance Metrics</h2>\\n");
        
        section.append(createMetricRow("Average Processing Time", 
            String.format("%.2f μs", stats.getAvgProcessingTimeNanos() / 1000.0)));
        section.append(createMetricRow("Slowest Log Processing", 
            String.format("%.2f ms", stats.getSlowestLogTimeNanos() / 1_000_000.0)));
        section.append(createMetricRow("Average Async Latency", 
            String.format("%.2f μs", collector.getAverageAsyncLatencyNanos() / 1000.0)));
        section.append(createMetricRow("Slowest Async Latency", 
            String.format("%.2f ms", collector.getSlowestAsyncLatencyNanos() / 1_000_000.0)));
        section.append(createMetricRow("Throughput", 
            String.format("%.2f logs/sec", collector.getThroughputPerSecond())));
        
        section.append("        </div>\\n");
        return section.toString();
    }
    
    private String createLogLevelSection(LoggingMetrics.LoggingStats stats) {
        StringBuilder section = new StringBuilder();
        section.append("        <div class=\"section\">\\n");
        section.append("            <h2>📊 Log Level Breakdown</h2>\\n");
        
        long totalLogs = stats.getTotalLogs();
        for (LogLevel level : LogLevel.values()) {
            Long count = stats.getLogLevelCounts().get(level);
            if (count != null && count > 0) {
                double percentage = totalLogs > 0 ? (double) count / totalLogs * 100 : 0;
                String levelClass = getLevelClass(level);
                
                section.append("            <div class=\"metric-row\">\\n");
                section.append("                <span class=\"metric-name ").append(levelClass).append("\">").append(level).append("</span>\\n");
                section.append("                <span class=\"metric-value\">").append(formatNumber(count)).append(" (").append(String.format("%.1f%%", percentage)).append(")</span>\\n");
                section.append("            </div>\\n");
                
                section.append("            <div class=\"progress-bar\">\\n");
                section.append("                <div class=\"progress-fill\" style=\"width: ").append(String.format("%.1f%%", percentage)).append("\"></div>\\n");
                section.append("            </div>\\n");
            }
        }
        
        section.append("        </div>\\n");
        return section.toString();
    }
    
    private String createLoggerSection(LoggingMetrics.LoggingStats stats) {
        StringBuilder section = new StringBuilder();
        section.append("        <div class=\"section\">\\n");
        section.append("            <h2>📝 Top Loggers</h2>\\n");
        
        long totalLogs = stats.getTotalLogs();
        stats.getLoggerCounts().entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .forEach(entry -> {
                    double percentage = totalLogs > 0 ? (double) entry.getValue() / totalLogs * 100 : 0;
                    section.append(createMetricRow(truncateString(entry.getKey(), 50), 
                        formatNumber(entry.getValue()) + String.format(" (%.1f%%)", percentage)));
                });
        
        section.append("        </div>\\n");
        return section.toString();
    }
    
    private String createHandlerSection(LoggingMetrics.LoggingStats stats) {
        StringBuilder section = new StringBuilder();
        section.append("        <div class=\"section\">\\n");
        section.append("            <h2>🔧 Handler Usage</h2>\\n");
        
        stats.getHandlerCounts().forEach((handler, count) -> {
            section.append(createMetricRow(handler, formatNumber(count)));
        });
        
        section.append("        </div>\\n");
        return section.toString();
    }
    
    private String createErrorSection() {
        StringBuilder section = new StringBuilder();
        Map<String, Long> errors = collector.getErrorCounts();
        
        if (!errors.isEmpty()) {
            section.append("        <div class=\"section\">\\n");
            section.append("            <h2>❌ Error Breakdown</h2>\\n");
            
            errors.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(10)
                    .forEach(entry -> {
                        section.append(createMetricRow(entry.getKey(), formatNumber(entry.getValue()), "error"));
                    });
            
            section.append("        </div>\\n");
        }
        
        return section.toString();
    }
    
    private String createQueueSection() {
        StringBuilder section = new StringBuilder();
        Map<String, Long> queues = collector.getQueueSizes();
        
        if (!queues.isEmpty()) {
            section.append("        <div class=\"section\">\\n");
            section.append("            <h2>📊 Queue Status</h2>\\n");
            
            queues.forEach((queue, size) -> {
                section.append(createMetricRow(queue, formatNumber(size)));
            });
            
            section.append("        </div>\\n");
        }
        
        return section.toString();
    }
    
    private String createSystemSection() {
        StringBuilder section = new StringBuilder();
        section.append("        <div class=\"section\">\\n");
        section.append("            <h2>💾 System Information</h2>\\n");
        
        section.append(createMetricRow("Current Memory Usage", 
            String.format("%.2f MB", collector.getCurrentMemoryUsage() / 1024.0 / 1024.0)));
        section.append(createMetricRow("Peak Memory Usage", 
            String.format("%.2f MB", collector.getPeakMemoryUsage() / 1024.0 / 1024.0)));
        section.append(createMetricRow("Handler Failures", 
            formatNumber(collector.getHandlerFailures())));
        section.append(createMetricRow("Configuration Changes", 
            formatNumber(collector.getConfigurationChanges())));
        
        section.append("        </div>\\n");
        return section.toString();
    }
    
    private String createMetricRow(String name, String value) {
        return createMetricRow(name, value, "");
    }
    
    private String createMetricRow(String name, String value, String cssClass) {
        return String.format(
            "            <div class=\"metric-row\">\\n" +
            "                <span class=\"metric-name\">%s</span>\\n" +
            "                <span class=\"metric-value %s\">%s</span>\\n" +
            "            </div>\\n",
            escapeHtml(name), cssClass, escapeHtml(value)
        );
    }
    
    private String getLevelClass(LogLevel level) {
        return switch (level) {
            case ERROR, CRITICAL -> "error";
            case WARN -> "warning";
            case INFO -> "info";
            case DEBUG -> "info";
            default -> "";
        };
    }
    
    private String formatNumber(long number) {
        if (number >= 1_000_000) {
            return String.format("%.1fM", number / 1_000_000.0);
        } else if (number >= 1_000) {
            return String.format("%.1fK", number / 1_000.0);
        } else {
            return String.valueOf(number);
        }
    }
    
    private String truncateString(String str, int maxLength) {
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength - 3) + "...";
    }
    
    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
    
    private String getChartScripts() {
        return """
            <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
            <script>
                // Add interactive charts here if needed
                // This is a placeholder for future chart implementation
            </script>
            """;
    }
}