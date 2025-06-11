package io.joshuasalcedo.logging.manager;

import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.handler.ConsoleHandler;
import io.joshuasalcedo.logging.handler.Handler;
import io.joshuasalcedo.logging.config.LoggingConfiguration;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class LoggerManager {
    private static final ConcurrentHashMap<String, io.joshuasalcedo.logging.core.Logger> loggers = new ConcurrentHashMap<>();
    private static final io.joshuasalcedo.logging.core.Logger rootLogger = new io.joshuasalcedo.logging.core.Logger("");
    private static boolean useAsyncByDefault = false; // Disabled for core module

    static {
        // Configure root logger with console handler by default
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(LogLevel.INFO);

        // Add shutdown hook for cleanup
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
        }, "LoggerManager-Shutdown"));
    }

    public static io.joshuasalcedo.logging.core.Logger getLogger(String name) {
        io.joshuasalcedo.logging.core.Logger existing = loggers.get(name);
        if (existing != null) {
            return existing;
        }

        // Create new logger
        io.joshuasalcedo.logging.core.Logger logger = new io.joshuasalcedo.logging.core.Logger(name);

        // Set up parent hierarchy
        String parentName = getParentName(name);
        if (parentName != null) {
            logger.setParent(getLogger(parentName));
        } else {
            logger.setParent(rootLogger);
        }

        // Try to put it in the map
        io.joshuasalcedo.logging.core.Logger previous = loggers.putIfAbsent(name, logger);
        return previous != null ? previous : logger;
    }

    public static io.joshuasalcedo.logging.core.Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static io.joshuasalcedo.logging.core.Logger getRootLogger() {
        return rootLogger;
    }

    private static String getParentName(String name) {
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : null;
    }

    // Configuration method that accepts LoggingConfiguration
    public static void configure(LoggingConfiguration config) {
        // Clear existing handlers
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
            handler.close();
        }

        // Add console handler if enabled
        if (config.isConsoleEnabled()) {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            consoleHandler.setLevel(config.getConsoleLevel());
            rootLogger.addHandler(consoleHandler);
        }

        // Note: Async and Database handlers are in separate modules
        // They would be configured through their respective modules
    }

    // Convenience method to configure basic console logging
    public static void configureConsoleLogging(LogLevel level) {
        // Clear existing handlers
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
            handler.close();
        }

        // Add console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(level);
        rootLogger.addHandler(consoleHandler);
    }

    // Add a handler to the root logger
    public static void addRootHandler(Handler handler) {
        rootLogger.addHandler(handler);
    }

    // Remove a handler from the root logger
    public static void removeRootHandler(Handler handler) {
        rootLogger.removeHandler(handler);
        handler.close();
    }

    // Get all root handlers
    public static List<Handler> getRootHandlers() {
        return rootLogger.getHandlers();
    }

    // Set root logger level
    public static void setRootLevel(LogLevel level) {
        rootLogger.setLevel(level);
    }

    // Get root logger level
    public static LogLevel getRootLevel() {
        return rootLogger.getLevel();
    }

    // Shutdown method to properly close all handlers
    public static void shutdown() {
        // Close all logger handlers
        for (io.joshuasalcedo.logging.core.Logger logger : loggers.values()) {
            for (Handler handler : logger.getHandlers()) {
                try {
                    handler.close();
                } catch (Exception e) {
                    System.err.println("Error closing handler: " + e.getMessage());
                }
            }
        }

        // Close root logger handlers
        for (Handler handler : rootLogger.getHandlers()) {
            try {
                handler.close();
            } catch (Exception e) {
                System.err.println("Error closing root handler: " + e.getMessage());
            }
        }
    }

    // Get all registered loggers
    public static List<io.joshuasalcedo.logging.core.Logger> getAllLoggers() {
        List<io.joshuasalcedo.logging.core.Logger> result = new ArrayList<>(loggers.values());
        result.add(rootLogger);
        return result;
    }

    // Clear all loggers (useful for testing)
    public static void reset() {
        shutdown();
        loggers.clear();

        // Reset root logger
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
        }

        // Re-add default console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();
        rootLogger.addHandler(consoleHandler);
        rootLogger.setLevel(LogLevel.INFO);
    }

    // Check if async is enabled (always false in core module)
    public static boolean isUseAsyncByDefault() {
        return useAsyncByDefault;
    }

    // Setter for async (no-op in core module, would be overridden by async module)
    public static void setUseAsyncByDefault(boolean useAsync) {
        if (useAsync) {
            System.out.println("Warning: Async support requires the logging-async module. " +
                             "Add logging-async dependency to enable async functionality.");
        }
        // For core module, always keep this false
        useAsyncByDefault = false;
    }
}