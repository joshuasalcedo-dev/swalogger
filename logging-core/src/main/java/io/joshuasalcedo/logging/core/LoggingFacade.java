package io.joshuasalcedo.logging.core;

import io.joshuasalcedo.logging.manager.LoggerManager;
import io.joshuasalcedo.logging.config.LoggingConfiguration;

/**
 * Main entry point for the logging library
 * Provides a simple API for common use cases
 */
public final class LoggingFacade {
    
    private LoggingFacade() {}
    
    /**
     * Get a logger for the calling class
     */
    public static Logger getLogger() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        return LoggerManager.getLogger(caller.getClassName());
    }
    
    /**
     * Get a logger by name
     */
    public static Logger getLogger(String name) {
        return LoggerManager.getLogger(name);
    }
    
    /**
     * Get a logger for a class
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerManager.getLogger(clazz);
    }
    
    /**
     * Configure logging with default settings
     * Sets up console logging with INFO level and basic formatting
     */
    public static void configure() {
        try {
            LoggingConfiguration defaultConfig = new LoggingConfiguration();
            applyConfiguration(defaultConfig);
        } catch (Exception e) {
            System.err.println("Failed to apply default logging configuration: " + e.getMessage());
            // Fallback to minimal configuration
            LoggerManager.configureConsoleLogging(LogLevel.INFO);
        }
    }
    
    /**
     * Configure logging from configuration file
     * Supports loading from classpath or filesystem
     * 
     * @param configFile Path to configuration file (e.g., "logging.properties")
     */
    public static void configure(String configFile) {
        if (configFile == null || configFile.trim().isEmpty()) {
            System.err.println("Configuration file path cannot be null or empty, using default configuration");
            configure();
            return;
        }
        
        try {
            LoggingConfiguration config = new LoggingConfiguration(configFile);
            applyConfiguration(config);
        } catch (java.io.IOException e) {
            System.err.println("Failed to load configuration from file '" + configFile + "': " + e.getMessage());
            System.err.println("Falling back to default configuration");
            configure();
        } catch (Exception e) {
            System.err.println("Error applying configuration from file '" + configFile + "': " + e.getMessage());
            System.err.println("Falling back to default configuration");
            configure();
        }
    }
    
    /**
     * Configure logging with a pre-built configuration object
     * 
     * @param config The logging configuration to apply
     */
    public static void configure(LoggingConfiguration config) {
        if (config == null) {
            System.err.println("Configuration cannot be null, using default configuration");
            configure();
            return;
        }
        
        try {
            applyConfiguration(config);
        } catch (Exception e) {
            System.err.println("Failed to apply provided configuration: " + e.getMessage());
            System.err.println("Falling back to default configuration");
            configure();
        }
    }
    
    /**
     * Internal method to apply a configuration to the logging system
     */
    private static void applyConfiguration(LoggingConfiguration config) {
        // Reset the logging system
        LoggerManager.reset();
        
        // Apply the configuration to LoggerManager
        LoggerManager.configure(config);
        
        // Set async behavior if specified
        if (config.isAsyncEnabled()) {
            LoggerManager.setUseAsyncByDefault(true);
        }
        
        // Set root logger level based on console level (as a reasonable default)
        LoggerManager.setRootLevel(config.getConsoleLevel());
    }
    
    /**
     * Get current logging configuration status
     * Returns a summary of the current logging setup
     */
    public static String getConfigurationStatus() {
        StringBuilder status = new StringBuilder();
        status.append("=== Logging Configuration Status ===\n");
        status.append("Root Logger Level: ").append(LoggerManager.getRootLevel()).append("\n");
        status.append("Async Enabled: ").append(LoggerManager.isUseAsyncByDefault()).append("\n");
        status.append("Active Handlers: ").append(LoggerManager.getRootHandlers().size()).append("\n");
        
        for (int i = 0; i < LoggerManager.getRootHandlers().size(); i++) {
            var handler = LoggerManager.getRootHandlers().get(i);
            status.append("  Handler ").append(i + 1).append(": ")
                  .append(handler.getClass().getSimpleName())
                  .append(" (Level: ").append(handler.getLevel()).append(")\n");
        }
        
        status.append("Total Loggers: ").append(LoggerManager.getAllLoggers().size()).append("\n");
        
        return status.toString();
    }
    
    /**
     * Reload configuration from the default configuration file
     * Attempts to load from "logging.properties" in classpath
     */
    public static void reloadConfiguration() {
        configure("logging.properties");
    }
    
    /**
     * Quick setup method for development/testing
     * Enables console logging with DEBUG level and colored output
     */
    public static void setupDevelopmentLogging() {
        LoggerManager.reset();
        
        // Add JLine handler for colored console output
        try {
            Class<?> jlineHandlerClass = Class.forName("io.joshuasalcedo.logging.handler.JLineHandler");
            var jlineHandler = (io.joshuasalcedo.logging.handler.Handler) jlineHandlerClass.getDeclaredConstructor().newInstance();
            jlineHandler.setLevel(LogLevel.DEBUG);
            LoggerManager.addRootHandler(jlineHandler);
        } catch (Exception e) {
            // Fallback to console handler if JLine is not available
            var consoleHandler = new io.joshuasalcedo.logging.handler.ConsoleHandler();
            consoleHandler.setLevel(LogLevel.DEBUG);
            LoggerManager.addRootHandler(consoleHandler);
        }
        
        LoggerManager.setRootLevel(LogLevel.DEBUG);
    }
    
    /**
     * Quick setup method for production
     * Enables conservative logging with INFO level and file output
     */
    public static void setupProductionLogging() {
        LoggerManager.reset();
        
        // Add console handler with conservative settings
        var consoleHandler = new io.joshuasalcedo.logging.handler.ConsoleHandler();
        consoleHandler.setLevel(LogLevel.WARN);  // Only warnings and errors to console
        LoggerManager.addRootHandler(consoleHandler);
        
        // Try to add file handler
        try {
            var fileHandler = new io.joshuasalcedo.logging.handler.FileHandler("logs/application.log");
            fileHandler.setLevel(LogLevel.INFO);
            LoggerManager.addRootHandler(fileHandler);
        } catch (Exception e) {
            System.err.println("Warning: Could not create file handler: " + e.getMessage());
        }
        
        LoggerManager.setRootLevel(LogLevel.INFO);
        LoggerManager.setUseAsyncByDefault(true);
    }
}
