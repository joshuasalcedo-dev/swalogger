package io.joshuasalcedo.logging.config;

import io.joshuasalcedo.logging.core.LogLevel;
import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoggingConfiguration {
    private final Properties properties;
    private final Map<String, LogLevel> loggerLevels;
    
    public LoggingConfiguration() {
        this.properties = new Properties();
        this.loggerLevels = new HashMap<>();
        loadDefaultConfiguration();
    }
    
    public LoggingConfiguration(String configFile) throws IOException {
        this();
        loadFromFile(configFile);
    }
    
    private void loadDefaultConfiguration() {
        // Set reasonable defaults
        properties.setProperty("logging.async.enabled", "true");
        properties.setProperty("logging.async.queueSize", "10000");
        properties.setProperty("logging.async.threadCount", "2");
        properties.setProperty("logging.console.enabled", "true");
        properties.setProperty("logging.console.level", "INFO");
        properties.setProperty("logging.file.enabled", "false");
        properties.setProperty("logging.database.enabled", "false");
        properties.setProperty("logging.pattern", "[%level][%timestamp] %logger.%method() - %message");
    }
    
    private void loadFromFile(String configFile) throws IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (is != null) {
                properties.load(is);
                parseLoggerLevels();
            }
        }
    }
    
    private void parseLoggerLevels() {
        properties.entrySet().stream()
            .filter(entry -> entry.getKey().toString().startsWith("logger."))
            .forEach(entry -> {
                String loggerName = entry.getKey().toString().substring(7); // Remove "logger."
                LogLevel level = LogLevel.valueOf(entry.getValue().toString().toUpperCase());
                loggerLevels.put(loggerName, level);
            });
    }
    
    // Getters for configuration values
    public boolean isAsyncEnabled() {
        return Boolean.parseBoolean(properties.getProperty("logging.async.enabled", "true"));
    }
    
    public int getAsyncQueueSize() {
        return Integer.parseInt(properties.getProperty("logging.async.queueSize", "10000"));
    }
    
    public int getAsyncThreadCount() {
        return Integer.parseInt(properties.getProperty("logging.async.threadCount", "2"));
    }
    
    public boolean isConsoleEnabled() {
        return Boolean.parseBoolean(properties.getProperty("logging.console.enabled", "true"));
    }
    
    public LogLevel getConsoleLevel() {
        return LogLevel.valueOf(properties.getProperty("logging.console.level", "INFO"));
    }
    
    public boolean isFileEnabled() {
        return Boolean.parseBoolean(properties.getProperty("logging.file.enabled", "false"));
    }
    
    public String getFilePattern() {
        return properties.getProperty("logging.file.pattern", "logs/app-%d{yyyy-MM-dd}.log");
    }
    
    public boolean isDatabaseEnabled() {
        return Boolean.parseBoolean(properties.getProperty("logging.database.enabled", "false"));
    }
    
    public String getDatabaseName() {
        return properties.getProperty("logging.database.name", "logs");
    }
    
    public String getPattern() {
        return properties.getProperty("logging.pattern", "[%level][%timestamp] %logger.%method() - %message");
    }
    
    public LogLevel getLoggerLevel(String loggerName) {
        return loggerLevels.getOrDefault(loggerName, LogLevel.INFO);
    }
    
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }
    
    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }
}