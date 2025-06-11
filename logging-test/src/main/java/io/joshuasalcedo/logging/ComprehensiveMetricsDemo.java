package io.joshuasalcedo.logging;// Comprehensive Logging Metrics Demo for Joshua Salcedo Logging Framework
// This demonstrates all the advanced metrics capabilities


import io.joshuasalcedo.logging.core.Logger;
import io.joshuasalcedo.logging.core.LoggingFacade;
import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import io.joshuasalcedo.logging.metrics.collector.DefaultMetricsCollector;
import io.joshuasalcedo.logging.metrics.config.MetricsConfiguration;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ComprehensiveMetricsDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Joshua Salcedo Logging Framework - Comprehensive Metrics Demo ===\n");
        
        try {
            // 1. Setup and Configuration
            demonstrateConfiguration();
            
            // 2. Basic Metrics Collection
            demonstrateBasicMetrics();
            
            // 3. Performance Tracking
            demonstratePerformanceTracking();
            
            // 4. Error and Exception Tracking
            demonstrateErrorTracking();
            
            // 5. Multi-threaded Metrics
            demonstrateMultiThreadedMetrics();
            
            // 6. Export Capabilities
            demonstrateExportCapabilities();
            
            // 7. Real-time Monitoring
            demonstrateRealTimeMonitoring();
            
            // 8. Health Checks and Alerts
            demonstrateHealthChecks();
            
        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void demonstrateConfiguration() {
        System.out.println("1. === Configuration Demo ===");
        
        // Default configuration
        MetricsConfiguration defaultConfig = new MetricsConfiguration();
        System.out.println("Default: " + defaultConfig);
        
        // Development configuration
        MetricsConfiguration devConfig = MetricsConfiguration.forDevelopment();
        System.out.println("Development: " + devConfig);
        
        // Production configuration
        MetricsConfiguration prodConfig = MetricsConfiguration.forProduction();
        System.out.println("Production: " + prodConfig);
        
        // Custom configuration
        MetricsConfiguration customConfig = new MetricsConfiguration();
        customConfig.setAutoReportEnabled(true);
        customConfig.setAutoReportIntervalMinutes(1);
        customConfig.setMethodTrackingEnabled(true);
        
        System.out.println("Custom: " + customConfig);
        System.out.println();
    }
    
    private static void demonstrateBasicMetrics() {
        System.out.println("2. === Basic Metrics Collection Demo ===");
        
        LoggingFacade.setupDevelopmentLogging();
        Logger logger = LoggingFacade.getLogger("MetricsDemo");
        
        // Generate various log events
        logger.debug("Debug message for metrics");
        logger.info("Info message for metrics");
        logger.warning("Warning message for metrics");
        logger.error("Error message for metrics");
        logger.critical("Critical message for metrics");
        
        // Get and display basic metrics
        LoggingMetrics metrics = LoggingMetrics.getInstance();
        LoggingMetrics.LoggingStats stats = metrics.getStats();
        
        System.out.println("Total logs: " + stats.getTotalLogs());
        System.out.println("Log level breakdown:");
        stats.getLogLevelCounts().forEach((level, count) -> 
            System.out.println("  " + level + ": " + count));
        
        System.out.println("Logger statistics:");
        stats.getLoggerCounts().forEach((loggerName, count) -> 
            System.out.println("  " + loggerName + ": " + count));
        
        System.out.println();
    }
    
    private static void demonstratePerformanceTracking() {
        System.out.println("3. === Performance Tracking Demo ===");
        
        Logger logger = LoggingFacade.getLogger("PerformanceDemo");
        DefaultMetricsCollector collector = (DefaultMetricsCollector) LoggingMetrics.getInstance().getCollector();
        
        // Simulate varying performance
        for (int i = 0; i < 100; i++) {
            long startTime = System.nanoTime();
            
            // Simulate some work
            try {
                Thread.sleep(i % 10); // Variable delay
                logger.info("Performance test message " + i);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Record custom metrics
            if (i % 20 == 0) {
                collector.recordQueueSize("TestQueue", 50 + (i % 100));
            }
        }
        
        LoggingMetrics.LoggingStats stats = LoggingMetrics.getInstance().getStats();
        System.out.println("Performance Statistics:");
        System.out.printf("  Average processing time: %.2f μs%n", stats.getAvgProcessingTimeNanos() / 1000.0);
        System.out.printf("  Slowest log time: %.2f ms%n", stats.getSlowestLogTimeNanos() / 1_000_000.0);
        System.out.printf("  Throughput: %.2f logs/sec%n", collector.getThroughputPerSecond());
        System.out.printf("  Current memory usage: %.2f MB%n", collector.getCurrentMemoryUsage() / 1024.0 / 1024.0);
        
        System.out.println();
    }
    
    private static void demonstrateErrorTracking() {
        System.out.println("4. === Error and Exception Tracking Demo ===");
        
        Logger logger = LoggingFacade.getLogger("ErrorTrackingDemo");
        DefaultMetricsCollector collector = (DefaultMetricsCollector) LoggingMetrics.getInstance().getCollector();
        
        // Generate various types of errors
        try {
            throw new RuntimeException("Simulated runtime error");
        } catch (Exception e) {
            logger.error("Caught runtime exception", e);
            collector.recordError("RuntimeException", e);
        }
        
        try {
            throw new IllegalArgumentException("Simulated argument error");
        } catch (Exception e) {
            logger.error("Caught argument exception", e);
            collector.recordError("IllegalArgumentException", e);
        }
        
        // Simulate handler failures
        collector.recordHandlerFailure("FileHandler", new IOException("Disk full"));
        collector.recordHandlerFailure("DatabaseHandler", new java.sql.SQLException("Connection lost"));
        
        System.out.println("Error Statistics:");
        System.out.printf("  Error rate: %.2f%%%n", collector.getErrorRate());
        System.out.println("  Handler failures: " + collector.getHandlerFailures());
        System.out.println("  Error breakdown:");
        collector.getErrorCounts().forEach((errorType, count) -> 
            System.out.println("    " + errorType + ": " + count));
        
        System.out.println();
    }
    
    private static void demonstrateMultiThreadedMetrics() {
        System.out.println("5. === Multi-threaded Metrics Demo ===");
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        // Submit multiple concurrent logging tasks
        for (int i = 0; i < 5; i++) {
            final int threadId = i;
            executor.submit(() -> {
                Logger logger = LoggingFacade.getLogger("Thread-" + threadId);
                
                for (int j = 0; j < 20; j++) {
                    logger.info("Thread " + threadId + " message " + j);
                    
                    // Simulate some processing time
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        executor.shutdown();
        try {
            executor.awaitTermination(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        LoggingMetrics.LoggingStats stats = LoggingMetrics.getInstance().getStats();
        System.out.println("Multi-threaded Results:");
        System.out.println("  Total logs from all threads: " + stats.getTotalLogs());
        System.out.println("  Thread-specific logger counts:");
        stats.getLoggerCounts().entrySet().stream()
                .filter(entry -> entry.getKey().startsWith("Thread-"))
                .forEach(entry -> System.out.println("    " + entry.getKey() + ": " + entry.getValue()));
        
        System.out.println();
    }
    
    private static void demonstrateExportCapabilities() {
        System.out.println("6. === Export Capabilities Demo ===");
        
        LoggingMetrics metrics = LoggingMetrics.getInstance();
        DefaultMetricsCollector collector = (DefaultMetricsCollector) metrics.getCollector();
        
        try {
            // JSON Export
            System.out.println("JSON Export Sample:");
            String jsonExport = metrics.exportMetrics("json");
            System.out.println(jsonExport.substring(0, Math.min(300, jsonExport.length())) + "...");
            
            // CSV Export
            System.out.println("\nCSV Export Sample:");
            String csvExport = metrics.exportMetrics("csv");
            String[] csvLines = csvExport.split("\\n");
            for (int i = 0; i < Math.min(5, csvLines.length); i++) {
                System.out.println(csvLines[i]);
            }
            System.out.println("...");
            
            // Prometheus Export
            System.out.println("\nPrometheus Export Sample:");
            String prometheusExport = metrics.exportMetrics("prometheus");
            String[] promLines = prometheusExport.split("\\n");
            for (int i = 0; i < Math.min(10, promLines.length); i++) {
                System.out.println(promLines[i]);
            }
            System.out.println("...");
            
            // Export to files
            try (FileOutputStream fos = new FileOutputStream("metrics-export.json")) {
                metrics.exportMetrics("json", fos);
                System.out.println("\nExported metrics to metrics-export.json");
            }
            
            try (FileOutputStream fos = new FileOutputStream("metrics-export.html")) {
                metrics.exportMetrics("html", fos);
                System.out.println("Exported HTML dashboard to metrics-export.html");
            }
            
        } catch (IOException e) {
            System.err.println("Export error: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    private static void demonstrateRealTimeMonitoring() {
        System.out.println("7. === Real-time Monitoring Demo ===");
        
        LoggingMetrics metrics = LoggingMetrics.getInstance();
        
        // Add a metrics listener
        metrics.addListener((level, loggerName, handlerName) -> {
            if (level.getValue() >= io.joshuasalcedo.logging.core.LogLevel.ERROR.getValue()) {
                System.out.println("🚨 Alert: " + level + " from " + loggerName);
            }
        });
        
        // Enable auto-reporting for demo
        metrics.enableAutoReporting(1); // Every minute (shortened for demo)
        
        Logger logger = LoggingFacade.getLogger("MonitoringDemo");
        
        // Generate some activity with errors
        for (int i = 0; i < 10; i++) {
            logger.info("Normal operation " + i);
            
            if (i % 3 == 0) {
                logger.error("Simulated error " + i);
            }
            
            try {
                Thread.sleep(100); // Small delay
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        // Show real-time performance summary
        System.out.println("\nReal-time Performance Summary:");
        System.out.println(metrics.getPerformanceSummary());
        
        System.out.println();
    }
    
    private static void demonstrateHealthChecks() {
        System.out.println("8. === Health Checks and Alerts Demo ===");
        
        LoggingMetrics metrics = LoggingMetrics.getInstance();
        DefaultMetricsCollector collector = (DefaultMetricsCollector) metrics.getCollector();
        
        // Check initial health
        System.out.println("Initial health status: " + (metrics.isHealthy() ? "HEALTHY ✅" : "UNHEALTHY ❌"));
        
        // Generate some concerning metrics
        Logger logger = LoggingFacade.getLogger("HealthCheckDemo");
        
        // Simulate high error rate
        for (int i = 0; i < 10; i++) {
            if (i < 7) {
                collector.recordError("HighFrequencyError", new RuntimeException("Frequent error"));
            }
            logger.info("Health check message " + i);
        }
        
        // Check health after errors
        System.out.println("Health after errors: " + (metrics.isHealthy() ? "HEALTHY ✅" : "UNHEALTHY ❌"));
        System.out.printf("Current error rate: %.2f%%%n", collector.getErrorRate());
        
        // Show comprehensive report
        System.out.println("\n=== Final Comprehensive Report ===");
        System.out.println(collector.getDetailedReport());
        
        // Cleanup
        metrics.disableAutoReporting();
        
        System.out.println("\n=== Demo Complete ===");
        System.out.println("Check the generated files:");
        System.out.println("- metrics-export.json (JSON metrics data)");
        System.out.println("- metrics-export.html (Interactive dashboard)");
    }
}