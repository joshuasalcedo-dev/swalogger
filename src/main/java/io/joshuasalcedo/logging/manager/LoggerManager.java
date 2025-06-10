package io.joshuasalcedo.logging.manager;

import io.joshuasalcedo.logging.LogLevel;
import io.joshuasalcedo.logging.handler.AsyncHandler;
import io.joshuasalcedo.logging.handler.ConsoleHandler;
import io.joshuasalcedo.logging.handler.DatabaseHandler;
import io.joshuasalcedo.logging.handler.Handler;

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class LoggerManager {
    private static final ConcurrentHashMap<String, Logger> loggers = new ConcurrentHashMap<>();
    private static final Logger rootLogger = new Logger("");
    private static boolean useAsyncByDefault = true;
    private static final List<AsyncHandler> activeAsyncHandlers = new ArrayList<>();
    
    static {
        // Configure root logger with async console handler by default
        ConsoleHandler consoleHandler = new ConsoleHandler();
        Handler handler = wrapIfAsync(consoleHandler);
        rootLogger.addHandler(handler);
        rootLogger.setLevel(LogLevel.INFO);
        
        // Add shutdown hook to properly close async handlers
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            shutdown();
        }, "LoggerManager-Shutdown"));
    }
    
    public static Logger getLogger(String name) {
        Logger existing = loggers.get(name);
        if (existing != null) {
            return existing;
        }
        
        // Create new logger
        Logger logger = new Logger(name);
        
        // Set up parent hierarchy
        String parentName = getParentName(name);
        if (parentName != null) {
            logger.setParent(getLogger(parentName));
        } else {
            logger.setParent(rootLogger);
        }
        
        // Try to put it in the map
        Logger previous = loggers.putIfAbsent(name, logger);
        return previous != null ? previous : logger;
    }
    
    public static Logger getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }
    
    public static Logger getRootLogger() {
        return rootLogger;
    }
    
    private static String getParentName(String name) {
        int lastDot = name.lastIndexOf('.');
        return lastDot > 0 ? name.substring(0, lastDot) : null;
    }
    
    // Database handler configuration methods
    public static void enableDatabaseLogging(String databaseName) throws SQLException {
        DatabaseHandler dbHandler = new DatabaseHandler(databaseName);
        Handler handler = wrapIfAsync(dbHandler);
        rootLogger.addHandler(handler);
    }
    
    public static void enableDatabaseLogging(String databaseName, LogLevel level) throws SQLException {
        DatabaseHandler dbHandler = new DatabaseHandler(databaseName);
        dbHandler.setLevel(level);
        Handler handler = wrapIfAsync(dbHandler);
        rootLogger.addHandler(handler);
    }
    
    public static void enableDatabaseLogging(DatabaseHandler dbHandler) {
        Handler handler = wrapIfAsync(dbHandler);
        rootLogger.addHandler(handler);
    }
    
    public static DatabaseHandler getRootDatabaseHandler() {
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof DatabaseHandler) {
                return (DatabaseHandler) handler;
            } else if (handler instanceof AsyncHandler) {
                Handler wrapped = ((AsyncHandler) handler).getWrappedHandler();
                if (wrapped instanceof DatabaseHandler) {
                    return (DatabaseHandler) wrapped;
                }
            }
        }
        return null;
    }
    
    public static void disableDatabaseLogging() {
        // Find and remove database handler (might be wrapped in AsyncHandler)
        Handler toRemove = null;
        for (Handler handler : rootLogger.getHandlers()) {
            if (handler instanceof DatabaseHandler) {
                toRemove = handler;
                break;
            } else if (handler instanceof AsyncHandler) {
                Handler wrapped = ((AsyncHandler) handler).getWrappedHandler();
                if (wrapped instanceof DatabaseHandler) {
                    toRemove = handler;
                    break;
                }
            }
        }
        
        if (toRemove != null) {
            rootLogger.removeHandler(toRemove);
            toRemove.close();
        }
    }
    
    // Convenience method to configure both console and database logging
    public static void configureLogging(boolean enableConsole, boolean enableDatabase, String databaseName) throws SQLException {
        // Clear existing handlers
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
            handler.close();
        }
        
        // Add console handler if enabled
        if (enableConsole) {
            ConsoleHandler consoleHandler = new ConsoleHandler();
            Handler handler = wrapIfAsync(consoleHandler);
            rootLogger.addHandler(handler);
        }
        
        // Add database handler if enabled
        if (enableDatabase && databaseName != null) {
            enableDatabaseLogging(databaseName);
        }
    }
    
    // Async configuration methods
    public static void setUseAsyncByDefault(boolean useAsync) {
        useAsyncByDefault = useAsync;
    }
    
    public static boolean isUseAsyncByDefault() {
        return useAsyncByDefault;
    }
    
    private static Handler wrapIfAsync(Handler handler) {
        if (useAsyncByDefault && !(handler instanceof AsyncHandler)) {
            AsyncHandler asyncHandler = new AsyncHandler(handler);
            synchronized (activeAsyncHandlers) {
                activeAsyncHandlers.add(asyncHandler);
            }
            return asyncHandler;
        }
        return handler;
    }
    
    // Shutdown method to properly close all async handlers
    public static void shutdown() {
        synchronized (activeAsyncHandlers) {
            for (AsyncHandler handler : activeAsyncHandlers) {
                try {
                    handler.close();
                } catch (Exception e) {
                    System.err.println("Error closing async handler: " + e.getMessage());
                }
            }
            activeAsyncHandlers.clear();
        }
        
        // Close all logger handlers
        for (Logger logger : loggers.values()) {
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
    
    // Flush all async handlers
    public static void flush() {
        synchronized (activeAsyncHandlers) {
            for (AsyncHandler handler : activeAsyncHandlers) {
                handler.flush();
            }
        }
    }
}