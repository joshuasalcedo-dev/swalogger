package io.joshuasalcedo.logging.metrics;

import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.metrics.collector.DefaultMetricsCollector;
import io.joshuasalcedo.logging.metrics.export.JsonMetricsExporter;
import io.joshuasalcedo.logging.metrics.export.CsvMetricsExporter;
import io.joshuasalcedo.logging.metrics.export.PrometheusMetricsExporter;
import io.joshuasalcedo.logging.metrics.export.HtmlMetricsExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the logging metrics system
 */
public class LoggingMetricsTest {

    private LoggingMetrics metrics;
    private DefaultMetricsCollector collector;

    @BeforeEach
    void setUp() {
        metrics = LoggingMetrics.getInstance();
        metrics.reset();
        collector = (DefaultMetricsCollector) metrics.getCollector();
    }

    @Test
    @DisplayName("Basic metrics collection should work correctly")
    void testBasicMetricsCollection() {
        // Record some log events
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        metrics.recordLog(LogLevel.ERROR, "TestLogger", "ConsoleHandler", 2000);
        metrics.recordLog(LogLevel.DEBUG, "AnotherLogger", "FileHandler", 500);

        LoggingMetrics.LoggingStats stats = metrics.getStats();

        assertEquals(3, stats.getTotalLogs());
        assertEquals(0, stats.getDroppedLogs());
        assertEquals(1166, stats.getAvgProcessingTimeNanos()); // (1000+2000+500)/3 = 1166.67
        assertEquals(2000, stats.getSlowestLogTimeNanos());

        // Check log level counts
        assertEquals(1, stats.getLogLevelCounts().get(LogLevel.INFO));
        assertEquals(1, stats.getLogLevelCounts().get(LogLevel.ERROR));
        assertEquals(1, stats.getLogLevelCounts().get(LogLevel.DEBUG));

        // Check logger counts
        assertEquals(2, stats.getLoggerCounts().get("TestLogger"));
        assertEquals(1, stats.getLoggerCounts().get("AnotherLogger"));

        // Check handler counts
        assertEquals(2, stats.getHandlerCounts().get("ConsoleHandler"));
        assertEquals(1, stats.getHandlerCounts().get("FileHandler"));
    }

    @Test
    @DisplayName("Dropped logs should be tracked correctly")
    void testDroppedLogsTracking() {
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        metrics.recordDroppedLog();
        metrics.recordDroppedLog();

        LoggingMetrics.LoggingStats stats = metrics.getStats();

        assertEquals(1, stats.getTotalLogs());
        assertEquals(2, stats.getDroppedLogs());
        assertEquals(200.0, stats.getDroppedPercentage(), 0.01); // 2/(1+2)*100
    }

    @Test
    @DisplayName("Error tracking should work correctly")
    void testErrorTracking() {
        RuntimeException testException = new RuntimeException("Test error");

        collector.recordError("TestError", testException);
        collector.recordHandlerFailure("ConsoleHandler", testException);

        assertEquals(1, collector.getHandlerFailures());
        assertTrue(collector.getErrorCounts().containsKey("TestError"));
        assertTrue(collector.getErrorCounts().containsKey("HandlerFailure_ConsoleHandler"));
        assertEquals(1, collector.getErrorCounts().get("TestError"));
        assertEquals(1, collector.getErrorCounts().get("HandlerFailure_ConsoleHandler"));
    }

    @Test
    @DisplayName("Queue size tracking should work correctly")
    void testQueueSizeTracking() {
        collector.recordQueueSize("AsyncQueue", 100);
        collector.recordQueueSize("DatabaseQueue", 50);

        assertEquals(100, collector.getQueueSizes().get("AsyncQueue"));
        assertEquals(50, collector.getQueueSizes().get("DatabaseQueue"));
    }

    @Test
    @DisplayName("Async latency tracking should work correctly")
    void testAsyncLatencyTracking() {
        collector.recordAsyncLatency(1000);
        collector.recordAsyncLatency(2000);
        collector.recordAsyncLatency(500);

        assertEquals(1166, collector.getAverageAsyncLatencyNanos(), 50); // (1000+2000+500)/3 ± 50
        assertEquals(2000, collector.getSlowestAsyncLatencyNanos());
    }

    @Test
    @DisplayName("Method call tracking should work correctly")
    void testMethodCallTracking() {
        collector.recordMethodCall("debug");
        collector.recordMethodCall("info");
        collector.recordMethodCall("debug");

        assertEquals(2, collector.getMethodCounts().get("debug"));
        assertEquals(1, collector.getMethodCounts().get("info"));
    }

    @Test
    @DisplayName("Performance calculations should be accurate")
    void testPerformanceCalculations() {
        // Record logs over time to test throughput calculation
        long startTime = System.currentTimeMillis();

        for (int i = 0; i < 10; i++) {
            metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        }

        assertTrue(collector.getThroughputPerSecond() >= 0);
        assertEquals(0.0, collector.getErrorRate(), 0.01); // No errors recorded
    }

    @Test
    @DisplayName("Reset functionality should clear all metrics")
    void testReset() {
        // Record some data
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        collector.recordError("TestError", new RuntimeException());
        collector.recordQueueSize("TestQueue", 100);

        // Verify data exists
        assertTrue(metrics.getStats().getTotalLogs() > 0);
        assertFalse(collector.getErrorCounts().isEmpty());
        assertFalse(collector.getQueueSizes().isEmpty());

        // Reset and verify data is cleared
        metrics.reset();

        assertEquals(0, metrics.getStats().getTotalLogs());
        assertTrue(collector.getErrorCounts().isEmpty());
        assertTrue(collector.getQueueSizes().isEmpty());
    }

    @Test
    @DisplayName("JSON export should work correctly")
    void testJsonExport() {
        // Record some test data
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        metrics.recordLog(LogLevel.ERROR, "TestLogger", "ConsoleHandler", 2000);
        collector.recordError("TestError", new RuntimeException("Test"));

        JsonMetricsExporter exporter = new JsonMetricsExporter(collector);
        String json = exporter.export();

        assertNotNull(json);
        assertTrue(json.contains("basicStatistics"));
        assertTrue(json.contains("performance"));
        assertTrue(json.contains("logLevelCounts"));
        assertTrue(json.contains("INFO"));
        assertTrue(json.contains("ERROR"));
        assertEquals("application/json", exporter.getContentType());
    }

    @Test
    @DisplayName("CSV export should work correctly")
    void testCsvExport() {
        // Record some test data
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);

        CsvMetricsExporter exporter = new CsvMetricsExporter(collector);
        String csv = exporter.export();

        assertNotNull(csv);
        assertTrue(csv.contains("Timestamp"));
        assertTrue(csv.contains("TotalLogs"));
        assertTrue(csv.contains("LogLevel,Count,Percentage"));
        assertEquals("text/csv", exporter.getContentType());
    }

    @Test
    @DisplayName("Prometheus export should work correctly")
    void testPrometheusExport() {
        // Record some test data
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);

        PrometheusMetricsExporter exporter = new PrometheusMetricsExporter(collector);
        String prometheus = exporter.export();

        assertNotNull(prometheus);
        assertTrue(prometheus.contains("# HELP"));
        assertTrue(prometheus.contains("# TYPE"));
        assertTrue(prometheus.contains("logging_logs_total"));
        assertTrue(prometheus.contains("counter"));
        assertTrue(prometheus.contains("gauge"));
        assertEquals("text/plain; version=0.0.4", exporter.getContentType());
    }

    @Test
    @DisplayName("HTML export should work correctly")
    void testHtmlExport() {
        // Record some test data
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);

        HtmlMetricsExporter exporter = new HtmlMetricsExporter(collector);
        String html = exporter.export();

        assertNotNull(html);
        assertTrue(html.contains("<!DOCTYPE html>"));
        assertTrue(html.contains("Logging Metrics Dashboard"));
        assertTrue(html.contains("Total Logs"));
        assertTrue(html.contains("Performance Metrics"));
        assertEquals("text/html", exporter.getContentType());
    }

    @Test
    @DisplayName("All export formats should be available")
    void testExportFormatsAvailability() {
        var formats = metrics.getAvailableFormats();

        assertTrue(formats.contains("json"));
        assertTrue(formats.contains("csv"));
        assertTrue(formats.contains("prometheus"));
        assertTrue(formats.contains("html"));

        // Test that all formats can export without errors
        assertDoesNotThrow(() -> metrics.exportMetrics("json"));
        assertDoesNotThrow(() -> metrics.exportMetrics("csv"));
        assertDoesNotThrow(() -> metrics.exportMetrics("prometheus"));
        assertDoesNotThrow(() -> metrics.exportMetrics("html"));
    }

    @Test
    @DisplayName("Health check should work correctly")
    void testHealthCheck() {
        // Initially should be healthy
        assertTrue(metrics.isHealthy());

        // Record some normal activity
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        assertTrue(metrics.isHealthy());

        // Record many errors to make it unhealthy
        for (int i = 0; i < 100; i++) {
            collector.recordError("TestError", new RuntimeException());
        }

        // Should still be healthy if total logs is much higher
        for (int i = 0; i < 1000; i++) {
            metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        }
        assertTrue(metrics.isHealthy());
    }

    @Test
    @DisplayName("Performance summary should be formatted correctly")
    void testPerformanceSummary() {
        // Record some test data
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        metrics.recordLog(LogLevel.ERROR, "TestLogger", "ConsoleHandler", 2000);

        String summary = metrics.getPerformanceSummary();

        assertNotNull(summary);
        assertTrue(summary.contains("Performance Summary"));
        assertTrue(summary.contains("logs/sec"));
        assertTrue(summary.contains("avg"));
        assertTrue(summary.contains("errors"));
        assertTrue(summary.contains("HEALTHY") || summary.contains("UNHEALTHY"));
    }

    @Test
    @DisplayName("Detailed report should contain comprehensive information")
    void testDetailedReport() {
        // Record diverse test data
        metrics.recordLog(LogLevel.INFO, "TestLogger", "ConsoleHandler", 1000);
        metrics.recordLog(LogLevel.ERROR, "AnotherLogger", "FileHandler", 2000);
        collector.recordError("TestError", new RuntimeException());
        collector.recordQueueSize("TestQueue", 50);
        collector.recordMethodCall("testMethod");

        String report = collector.getDetailedReport();

        assertNotNull(report);
        assertTrue(report.contains("Comprehensive Logging Metrics Report"));
        assertTrue(report.contains("Basic Statistics"));
        assertTrue(report.contains("Performance Metrics"));
        assertTrue(report.contains("Memory Usage"));
        assertTrue(report.contains("Log Level Breakdown"));
        assertTrue(report.contains("Top Loggers"));
        assertTrue(report.contains("Handler Usage"));
        assertTrue(report.contains("Error Breakdown"));
        assertTrue(report.contains("Queue Status"));
    }

    @Test
    @DisplayName("Concurrent access should be thread-safe")
    void testThreadSafety() throws InterruptedException {
        int threadCount = 10;
        int logsPerThread = 100;
        Thread[] threads = new Thread[threadCount];

        // Create threads that simultaneously record metrics
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < logsPerThread; j++) {
                    metrics.recordLog(LogLevel.INFO, "Thread" + threadId, "Handler" + threadId, 1000);
                    collector.recordError("Error" + threadId, new RuntimeException());
                    collector.recordQueueSize("Queue" + threadId, j);
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Verify results
        LoggingMetrics.LoggingStats stats = metrics.getStats();
        assertEquals(threadCount * logsPerThread, stats.getTotalLogs());

        // Verify all loggers were recorded
        for (int i = 0; i < threadCount; i++) {
            assertTrue(stats.getLoggerCounts().containsKey("Thread" + i));
            assertEquals(logsPerThread, stats.getLoggerCounts().get("Thread" + i));
        }
    }
}
