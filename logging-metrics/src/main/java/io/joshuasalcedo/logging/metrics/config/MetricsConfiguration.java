package io.joshuasalcedo.logging.metrics.config;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.io.InputStream;
import java.io.IOException;

/**
 * Configuration class for logging metrics system
 */
public class MetricsConfiguration {
    private boolean enabled = true;
    private boolean autoReportEnabled = false;
    private int autoReportIntervalMinutes = 5;
    private long retentionHours = 24;
    private boolean exportEnabled = false;
    private String exportFormat = "json";
    private String exportPath = "logs/metrics";
    private int exportIntervalMinutes = 15;
    private boolean memoryTrackingEnabled = true;
    private boolean performanceTrackingEnabled = true;
    private boolean errorTrackingEnabled = true;
    private boolean queueTrackingEnabled = true;
    private boolean methodTrackingEnabled = false;
    private double errorRateThreshold = 5.0;
    private long slowLogThresholdNanos = 1_000_000; // 1ms
    private int maxLoggerMetrics = 100;
    private int maxErrorTypes = 50;

    public MetricsConfiguration() {
        // Default configuration
    }

    public MetricsConfiguration(String configFile) throws IOException {
        loadFromFile(configFile);
    }

    public MetricsConfiguration(Properties properties) {
        loadFromProperties(properties);
    }

    private void loadFromFile(String configFile) throws IOException {
        Properties props = new Properties();

        // Try to load from classpath first
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (is != null) {
                props.load(is);
                loadFromProperties(props);
                return;
            }
        }

        // Try to load from filesystem
        try (InputStream is = new java.io.FileInputStream(configFile)) {
            props.load(is);
            loadFromProperties(props);
        }
    }

    private void loadFromProperties(Properties props) {
        this.enabled = getBooleanProperty(props, "metrics.enabled", enabled);
        this.autoReportEnabled = getBooleanProperty(props, "metrics.auto.report.enabled", autoReportEnabled);
        this.autoReportIntervalMinutes = getIntProperty(props, "metrics.auto.report.interval.minutes", autoReportIntervalMinutes);
        this.retentionHours = getLongProperty(props, "metrics.retention.hours", retentionHours);
        this.exportEnabled = getBooleanProperty(props, "metrics.export.enabled", exportEnabled);
        this.exportFormat = getStringProperty(props, "metrics.export.format", exportFormat);
        this.exportPath = getStringProperty(props, "metrics.export.path", exportPath);
        this.exportIntervalMinutes = getIntProperty(props, "metrics.export.interval.minutes", exportIntervalMinutes);
        this.memoryTrackingEnabled = getBooleanProperty(props, "metrics.tracking.memory", memoryTrackingEnabled);
        this.performanceTrackingEnabled = getBooleanProperty(props, "metrics.tracking.performance", performanceTrackingEnabled);
        this.errorTrackingEnabled = getBooleanProperty(props, "metrics.tracking.errors", errorTrackingEnabled);
        this.queueTrackingEnabled = getBooleanProperty(props, "metrics.tracking.queues", queueTrackingEnabled);
        this.methodTrackingEnabled = getBooleanProperty(props, "metrics.tracking.methods", methodTrackingEnabled);
        this.errorRateThreshold = getDoubleProperty(props, "metrics.thresholds.error.rate", errorRateThreshold);
        this.slowLogThresholdNanos = getLongProperty(props, "metrics.thresholds.slow.log.nanos", slowLogThresholdNanos);
        this.maxLoggerMetrics = getIntProperty(props, "metrics.limits.max.loggers", maxLoggerMetrics);
        this.maxErrorTypes = getIntProperty(props, "metrics.limits.max.error.types", maxErrorTypes);
    }

    private boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        // Only accept "true" or "false" (case-insensitive) as valid boolean values
        if (value.equalsIgnoreCase("true")) {
            return true;
        } else if (value.equalsIgnoreCase("false")) {
            return false;
        } else {
            // Invalid boolean value, use default
            return defaultValue;
        }
    }

    private int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long getLongProperty(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private double getDoubleProperty(Properties props, String key, double defaultValue) {
        String value = props.getProperty(key);
        try {
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private String getStringProperty(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }

    // Getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isAutoReportEnabled() { return autoReportEnabled; }
    public void setAutoReportEnabled(boolean autoReportEnabled) { this.autoReportEnabled = autoReportEnabled; }

    public int getAutoReportIntervalMinutes() { return autoReportIntervalMinutes; }
    public void setAutoReportIntervalMinutes(int autoReportIntervalMinutes) { this.autoReportIntervalMinutes = autoReportIntervalMinutes; }

    public long getRetentionHours() { return retentionHours; }
    public void setRetentionHours(long retentionHours) { this.retentionHours = retentionHours; }

    public boolean isExportEnabled() { return exportEnabled; }
    public void setExportEnabled(boolean exportEnabled) { this.exportEnabled = exportEnabled; }

    public String getExportFormat() { return exportFormat; }
    public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }

    public String getExportPath() { return exportPath; }
    public void setExportPath(String exportPath) { this.exportPath = exportPath; }

    public int getExportIntervalMinutes() { return exportIntervalMinutes; }
    public void setExportIntervalMinutes(int exportIntervalMinutes) { this.exportIntervalMinutes = exportIntervalMinutes; }

    public boolean isMemoryTrackingEnabled() { return memoryTrackingEnabled; }
    public void setMemoryTrackingEnabled(boolean memoryTrackingEnabled) { this.memoryTrackingEnabled = memoryTrackingEnabled; }

    public boolean isPerformanceTrackingEnabled() { return performanceTrackingEnabled; }
    public void setPerformanceTrackingEnabled(boolean performanceTrackingEnabled) { this.performanceTrackingEnabled = performanceTrackingEnabled; }

    public boolean isErrorTrackingEnabled() { return errorTrackingEnabled; }
    public void setErrorTrackingEnabled(boolean errorTrackingEnabled) { this.errorTrackingEnabled = errorTrackingEnabled; }

    public boolean isQueueTrackingEnabled() { return queueTrackingEnabled; }
    public void setQueueTrackingEnabled(boolean queueTrackingEnabled) { this.queueTrackingEnabled = queueTrackingEnabled; }

    public boolean isMethodTrackingEnabled() { return methodTrackingEnabled; }
    public void setMethodTrackingEnabled(boolean methodTrackingEnabled) { this.methodTrackingEnabled = methodTrackingEnabled; }

    public double getErrorRateThreshold() { return errorRateThreshold; }
    public void setErrorRateThreshold(double errorRateThreshold) { this.errorRateThreshold = errorRateThreshold; }

    public long getSlowLogThresholdNanos() { return slowLogThresholdNanos; }
    public void setSlowLogThresholdNanos(long slowLogThresholdNanos) { this.slowLogThresholdNanos = slowLogThresholdNanos; }

    public int getMaxLoggerMetrics() { return maxLoggerMetrics; }
    public void setMaxLoggerMetrics(int maxLoggerMetrics) { this.maxLoggerMetrics = maxLoggerMetrics; }

    public int getMaxErrorTypes() { return maxErrorTypes; }
    public void setMaxErrorTypes(int maxErrorTypes) { this.maxErrorTypes = maxErrorTypes; }

    // Convenience methods
    public long getAutoReportIntervalMillis() {
        return TimeUnit.MINUTES.toMillis(autoReportIntervalMinutes);
    }

    public long getExportIntervalMillis() {
        return TimeUnit.MINUTES.toMillis(exportIntervalMinutes);
    }

    public long getRetentionMillis() {
        return TimeUnit.HOURS.toMillis(retentionHours);
    }

    public double getSlowLogThresholdMillis() {
        return slowLogThresholdNanos / 1_000_000.0;
    }

    /**
     * Validate configuration settings
     */
    public void validate() throws IllegalArgumentException {
        if (autoReportIntervalMinutes <= 0) {
            throw new IllegalArgumentException("Auto report interval must be positive");
        }
        if (retentionHours <= 0) {
            throw new IllegalArgumentException("Retention hours must be positive");
        }
        if (exportIntervalMinutes <= 0) {
            throw new IllegalArgumentException("Export interval must be positive");
        }
        if (errorRateThreshold < 0 || errorRateThreshold > 100) {
            throw new IllegalArgumentException("Error rate threshold must be between 0 and 100");
        }
        if (slowLogThresholdNanos <= 0) {
            throw new IllegalArgumentException("Slow log threshold must be positive");
        }
        if (maxLoggerMetrics <= 0) {
            throw new IllegalArgumentException("Max logger metrics must be positive");
        }
        if (maxErrorTypes <= 0) {
            throw new IllegalArgumentException("Max error types must be positive");
        }
    }

    /**
     * Convert configuration to Properties for serialization
     */
    public Properties toProperties() {
        Properties props = new Properties();
        props.setProperty("metrics.enabled", String.valueOf(enabled));
        props.setProperty("metrics.auto.report.enabled", String.valueOf(autoReportEnabled));
        props.setProperty("metrics.auto.report.interval.minutes", String.valueOf(autoReportIntervalMinutes));
        props.setProperty("metrics.retention.hours", String.valueOf(retentionHours));
        props.setProperty("metrics.export.enabled", String.valueOf(exportEnabled));
        props.setProperty("metrics.export.format", exportFormat);
        props.setProperty("metrics.export.path", exportPath);
        props.setProperty("metrics.export.interval.minutes", String.valueOf(exportIntervalMinutes));
        props.setProperty("metrics.tracking.memory", String.valueOf(memoryTrackingEnabled));
        props.setProperty("metrics.tracking.performance", String.valueOf(performanceTrackingEnabled));
        props.setProperty("metrics.tracking.errors", String.valueOf(errorTrackingEnabled));
        props.setProperty("metrics.tracking.queues", String.valueOf(queueTrackingEnabled));
        props.setProperty("metrics.tracking.methods", String.valueOf(methodTrackingEnabled));
        props.setProperty("metrics.thresholds.error.rate", String.valueOf(errorRateThreshold));
        props.setProperty("metrics.thresholds.slow.log.nanos", String.valueOf(slowLogThresholdNanos));
        props.setProperty("metrics.limits.max.loggers", String.valueOf(maxLoggerMetrics));
        props.setProperty("metrics.limits.max.error.types", String.valueOf(maxErrorTypes));
        return props;
    }

    @Override
    public String toString() {
        return String.format(
            "MetricsConfiguration{enabled=%s, autoReport=%s(%dm), export=%s(%s), tracking={memory=%s, perf=%s, errors=%s, queues=%s, methods=%s}}",
            enabled, autoReportEnabled, autoReportIntervalMinutes, exportEnabled, exportFormat,
            memoryTrackingEnabled, performanceTrackingEnabled, errorTrackingEnabled, queueTrackingEnabled, methodTrackingEnabled
        );
    }

    /**
     * Create a default configuration for development
     */
    public static MetricsConfiguration forDevelopment() {
        MetricsConfiguration config = new MetricsConfiguration();
        config.setEnabled(true);
        config.setAutoReportEnabled(true);
        config.setAutoReportIntervalMinutes(1); // More frequent for development
        config.setExportEnabled(false);
        config.setMethodTrackingEnabled(true); // Track method calls in dev
        config.setErrorRateThreshold(1.0); // Lower threshold for dev
        return config;
    }

    /**
     * Create a default configuration for production
     */
    public static MetricsConfiguration forProduction() {
        MetricsConfiguration config = new MetricsConfiguration();
        config.setEnabled(true);
        config.setAutoReportEnabled(false); // Disable auto reports in production
        config.setExportEnabled(true);
        config.setExportFormat("prometheus"); // Better for monitoring systems
        config.setExportIntervalMinutes(5);
        config.setMethodTrackingEnabled(false); // Disable expensive tracking
        config.setRetentionHours(48); // Longer retention for production
        return config;
    }

    /**
     * Create a minimal configuration for testing
     */
    public static MetricsConfiguration forTesting() {
        MetricsConfiguration config = new MetricsConfiguration();
        config.setEnabled(true);
        config.setAutoReportEnabled(false);
        config.setExportEnabled(false);
        config.setMemoryTrackingEnabled(false);
        config.setMethodTrackingEnabled(false);
        config.setRetentionHours(1); // Short retention for tests
        return config;
    }
}
