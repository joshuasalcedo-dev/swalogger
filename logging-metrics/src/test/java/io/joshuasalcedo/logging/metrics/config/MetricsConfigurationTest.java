package io.joshuasalcedo.logging.metrics.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MetricsConfiguration
 */
public class MetricsConfigurationTest {

    @Test
    @DisplayName("Default configuration should have reasonable values")
    void testDefaultConfiguration() {
        MetricsConfiguration config = new MetricsConfiguration();

        assertTrue(config.isEnabled());
        assertFalse(config.isAutoReportEnabled());
        assertEquals(5, config.getAutoReportIntervalMinutes());
        assertEquals(24, config.getRetentionHours());
        assertFalse(config.isExportEnabled());
        assertEquals("json", config.getExportFormat());
        assertEquals("logs/metrics", config.getExportPath());
        assertEquals(15, config.getExportIntervalMinutes());
        assertTrue(config.isMemoryTrackingEnabled());
        assertTrue(config.isPerformanceTrackingEnabled());
        assertTrue(config.isErrorTrackingEnabled());
        assertTrue(config.isQueueTrackingEnabled());
        assertFalse(config.isMethodTrackingEnabled());
        assertEquals(5.0, config.getErrorRateThreshold());
        assertEquals(1_000_000, config.getSlowLogThresholdNanos());
        assertEquals(100, config.getMaxLoggerMetrics());
        assertEquals(50, config.getMaxErrorTypes());
    }

    @Test
    @DisplayName("Configuration should load from properties")
    void testLoadFromProperties() {
        Properties props = new Properties();
        props.setProperty("metrics.enabled", "false");
        props.setProperty("metrics.auto.report.enabled", "true");
        props.setProperty("metrics.auto.report.interval.minutes", "10");
        props.setProperty("metrics.retention.hours", "48");
        props.setProperty("metrics.export.enabled", "true");
        props.setProperty("metrics.export.format", "prometheus");
        props.setProperty("metrics.export.path", "/tmp/metrics");
        props.setProperty("metrics.export.interval.minutes", "30");
        props.setProperty("metrics.tracking.memory", "false");
        props.setProperty("metrics.tracking.performance", "false");
        props.setProperty("metrics.tracking.errors", "false");
        props.setProperty("metrics.tracking.queues", "false");
        props.setProperty("metrics.tracking.methods", "true");
        props.setProperty("metrics.thresholds.error.rate", "10.5");
        props.setProperty("metrics.thresholds.slow.log.nanos", "5000000");
        props.setProperty("metrics.limits.max.loggers", "200");
        props.setProperty("metrics.limits.max.error.types", "75");

        MetricsConfiguration config = new MetricsConfiguration(props);

        assertFalse(config.isEnabled());
        assertTrue(config.isAutoReportEnabled());
        assertEquals(10, config.getAutoReportIntervalMinutes());
        assertEquals(48, config.getRetentionHours());
        assertTrue(config.isExportEnabled());
        assertEquals("prometheus", config.getExportFormat());
        assertEquals("/tmp/metrics", config.getExportPath());
        assertEquals(30, config.getExportIntervalMinutes());
        assertFalse(config.isMemoryTrackingEnabled());
        assertFalse(config.isPerformanceTrackingEnabled());
        assertFalse(config.isErrorTrackingEnabled());
        assertFalse(config.isQueueTrackingEnabled());
        assertTrue(config.isMethodTrackingEnabled());
        assertEquals(10.5, config.getErrorRateThreshold());
        assertEquals(5_000_000, config.getSlowLogThresholdNanos());
        assertEquals(200, config.getMaxLoggerMetrics());
        assertEquals(75, config.getMaxErrorTypes());
    }

    @Test
    @DisplayName("Configuration should load from file")
    void testLoadFromFile(@TempDir Path tempDir) throws IOException {
        Path configFile = tempDir.resolve("metrics.properties");

        String configContent = 
            "metrics.enabled=true\n" +
            "metrics.auto.report.enabled=true\n" +
            "metrics.auto.report.interval.minutes=2\n" +
            "metrics.export.enabled=true\n" +
            "metrics.export.format=csv\n" +
            "metrics.tracking.methods=true";

        Files.writeString(configFile, configContent);

        MetricsConfiguration config = new MetricsConfiguration(configFile.toString());

        assertTrue(config.isEnabled());
        assertTrue(config.isAutoReportEnabled());
        assertEquals(2, config.getAutoReportIntervalMinutes());
        assertTrue(config.isExportEnabled());
        assertEquals("csv", config.getExportFormat());
        assertTrue(config.isMethodTrackingEnabled());
    }

    @Test
    @DisplayName("Invalid properties should use default values")
    void testInvalidProperties() {
        Properties props = new Properties();
        props.setProperty("metrics.enabled", "invalid");
        props.setProperty("metrics.auto.report.interval.minutes", "not-a-number");
        props.setProperty("metrics.thresholds.error.rate", "invalid-double");

        MetricsConfiguration config = new MetricsConfiguration(props);

        // Should use default values for invalid properties
        assertTrue(config.isEnabled()); // default
        assertEquals(5, config.getAutoReportIntervalMinutes()); // default
        assertEquals(5.0, config.getErrorRateThreshold()); // default
    }

    @Test
    @DisplayName("Convenience methods should work correctly")
    void testConvenienceMethods() {
        MetricsConfiguration config = new MetricsConfiguration();
        config.setAutoReportIntervalMinutes(10);
        config.setExportIntervalMinutes(30);
        config.setRetentionHours(48);
        config.setSlowLogThresholdNanos(2_000_000);

        assertEquals(10 * 60 * 1000, config.getAutoReportIntervalMillis());
        assertEquals(30 * 60 * 1000, config.getExportIntervalMillis());
        assertEquals(48 * 60 * 60 * 1000, config.getRetentionMillis());
        assertEquals(2.0, config.getSlowLogThresholdMillis());
    }

    @Test
    @DisplayName("Validation should catch invalid configurations")
    void testValidation() {
        MetricsConfiguration config = new MetricsConfiguration();

        // Valid configuration should pass
        assertDoesNotThrow(config::validate);

        // Invalid configurations should fail
        config.setAutoReportIntervalMinutes(0);
        assertThrows(IllegalArgumentException.class, config::validate);

        config.setAutoReportIntervalMinutes(5); // Reset to valid
        config.setRetentionHours(-1);
        assertThrows(IllegalArgumentException.class, config::validate);

        config.setRetentionHours(24); // Reset to valid
        config.setErrorRateThreshold(-1);
        assertThrows(IllegalArgumentException.class, config::validate);

        config.setErrorRateThreshold(101);
        assertThrows(IllegalArgumentException.class, config::validate);

        config.setErrorRateThreshold(5.0); // Reset to valid
        config.setSlowLogThresholdNanos(0);
        assertThrows(IllegalArgumentException.class, config::validate);

        config.setSlowLogThresholdNanos(1000); // Reset to valid
        config.setMaxLoggerMetrics(0);
        assertThrows(IllegalArgumentException.class, config::validate);

        config.setMaxLoggerMetrics(100); // Reset to valid
        config.setMaxErrorTypes(0);
        assertThrows(IllegalArgumentException.class, config::validate);
    }

    @Test
    @DisplayName("Configuration should convert to and from Properties")
    void testPropertiesConversion() {
        MetricsConfiguration original = new MetricsConfiguration();
        original.setEnabled(false);
        original.setAutoReportEnabled(true);
        original.setAutoReportIntervalMinutes(15);
        original.setExportFormat("prometheus");
        original.setMethodTrackingEnabled(true);

        Properties props = original.toProperties();
        MetricsConfiguration restored = new MetricsConfiguration(props);

        assertEquals(original.isEnabled(), restored.isEnabled());
        assertEquals(original.isAutoReportEnabled(), restored.isAutoReportEnabled());
        assertEquals(original.getAutoReportIntervalMinutes(), restored.getAutoReportIntervalMinutes());
        assertEquals(original.getExportFormat(), restored.getExportFormat());
        assertEquals(original.isMethodTrackingEnabled(), restored.isMethodTrackingEnabled());
    }

    @Test
    @DisplayName("Predefined configurations should be reasonable")
    void testPredefinedConfigurations() {
        // Development configuration
        MetricsConfiguration dev = MetricsConfiguration.forDevelopment();
        assertTrue(dev.isEnabled());
        assertTrue(dev.isAutoReportEnabled());
        assertEquals(1, dev.getAutoReportIntervalMinutes()); // More frequent for dev
        assertFalse(dev.isExportEnabled());
        assertTrue(dev.isMethodTrackingEnabled()); // More detailed tracking
        assertEquals(1.0, dev.getErrorRateThreshold()); // Lower threshold

        // Production configuration
        MetricsConfiguration prod = MetricsConfiguration.forProduction();
        assertTrue(prod.isEnabled());
        assertFalse(prod.isAutoReportEnabled()); // No console spam in prod
        assertTrue(prod.isExportEnabled());
        assertEquals("prometheus", prod.getExportFormat()); // Better for monitoring
        assertEquals(5, prod.getExportIntervalMinutes());
        assertFalse(prod.isMethodTrackingEnabled()); // Less overhead
        assertEquals(48, prod.getRetentionHours()); // Longer retention

        // Testing configuration
        MetricsConfiguration test = MetricsConfiguration.forTesting();
        assertTrue(test.isEnabled());
        assertFalse(test.isAutoReportEnabled());
        assertFalse(test.isExportEnabled());
        assertFalse(test.isMemoryTrackingEnabled());
        assertFalse(test.isMethodTrackingEnabled());
        assertEquals(1, test.getRetentionHours()); // Short retention

        // All should validate successfully
        assertDoesNotThrow(dev::validate);
        assertDoesNotThrow(prod::validate);
        assertDoesNotThrow(test::validate);
    }

    @Test
    @DisplayName("toString should provide readable output")
    void testToString() {
        MetricsConfiguration config = new MetricsConfiguration();
        config.setEnabled(true);
        config.setAutoReportEnabled(true);
        config.setAutoReportIntervalMinutes(10);
        config.setExportEnabled(true);
        config.setExportFormat("json");

        String toString = config.toString();

        assertNotNull(toString);
        assertTrue(toString.contains("MetricsConfiguration"));
        assertTrue(toString.contains("enabled=true"));
        assertTrue(toString.contains("autoReport=true"));
        assertTrue(toString.contains("10m"));
        assertTrue(toString.contains("export=true"));
        assertTrue(toString.contains("json"));
    }

    @Test
    @DisplayName("Setters should update values correctly")
    void testSetters() {
        MetricsConfiguration config = new MetricsConfiguration();

        config.setEnabled(false);
        assertFalse(config.isEnabled());

        config.setAutoReportEnabled(true);
        assertTrue(config.isAutoReportEnabled());

        config.setAutoReportIntervalMinutes(20);
        assertEquals(20, config.getAutoReportIntervalMinutes());

        config.setRetentionHours(72);
        assertEquals(72, config.getRetentionHours());

        config.setExportEnabled(true);
        assertTrue(config.isExportEnabled());

        config.setExportFormat("csv");
        assertEquals("csv", config.getExportFormat());

        config.setExportPath("/custom/path");
        assertEquals("/custom/path", config.getExportPath());

        config.setExportIntervalMinutes(45);
        assertEquals(45, config.getExportIntervalMinutes());

        config.setMemoryTrackingEnabled(false);
        assertFalse(config.isMemoryTrackingEnabled());

        config.setPerformanceTrackingEnabled(false);
        assertFalse(config.isPerformanceTrackingEnabled());

        config.setErrorTrackingEnabled(false);
        assertFalse(config.isErrorTrackingEnabled());

        config.setQueueTrackingEnabled(false);
        assertFalse(config.isQueueTrackingEnabled());

        config.setMethodTrackingEnabled(true);
        assertTrue(config.isMethodTrackingEnabled());

        config.setErrorRateThreshold(15.5);
        assertEquals(15.5, config.getErrorRateThreshold());

        config.setSlowLogThresholdNanos(3_000_000);
        assertEquals(3_000_000, config.getSlowLogThresholdNanos());

        config.setMaxLoggerMetrics(500);
        assertEquals(500, config.getMaxLoggerMetrics());

        config.setMaxErrorTypes(100);
        assertEquals(100, config.getMaxErrorTypes());
    }

    @Test
    @DisplayName("Configuration should handle missing file gracefully")
    void testMissingConfigFile() {
        assertThrows(IOException.class, () -> {
            new MetricsConfiguration("/path/that/does/not/exist.properties");
        });
    }
}
