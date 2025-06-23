package io.joshuasalcedo.logging.manager;

import io.joshuasalcedo.logging.Log;
import io.joshuasalcedo.logging.LogLevel;
import io.joshuasalcedo.logging.handler.Handler;
import io.joshuasalcedo.logging.handler.DatabaseHandler;
import io.joshuasalcedo.logging.handler.AsyncHandler;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class Logger {
    private final String name;
    private LogLevel level = LogLevel.INFO;
    private final List<Handler> handlers = new CopyOnWriteArrayList<>();
    private Logger parent;
    private boolean useParentHandlers = true;
    
    protected Logger(String name) {
        this.name = name;
    }
    
    public void log(LogLevel level, String message) {
        if (level.getValue() < this.level.getValue()) {
            return;
        }
        
        Log record = new Log(level, message, name);
        publish(record);
    }
    
    public void log(LogLevel level, String message, Throwable throwable) {
        if (level.getValue() < this.level.getValue()) {
            return;
        }
        
        Log record = new Log(level, message, name, throwable);
        publish(record);
    }
    
    private void publish(Log record) {
        // Publish to this logger's handlers
        for (Handler handler : handlers) {
            handler.publish(record);
        }
        
        // Publish to parent handlers if enabled
        if (useParentHandlers && parent != null) {
            parent.publish(record);
        }
    }
    
    // Convenience methods
    public void debug(String message) {
        log(LogLevel.DEBUG, message);
    }
    
    public void info(String message) {
        log(LogLevel.INFO, message);
    }
    
    public void warning(String message) {
        log(LogLevel.WARN, message);
    }
    
    public void error(String message) {
        log(LogLevel.ERROR, message);
    }
    
    public void error(String message, Throwable throwable) {
        log(LogLevel.ERROR, message, throwable);
    }
    
    public void critical(String message) {
        log(LogLevel.CRITICAL, message);
    }
    
    // Handler management
    public void addHandler(Handler handler) {
        handlers.add(handler);
    }
    
    public void removeHandler(Handler handler) {
        handlers.remove(handler);
    }
    
    public List<Handler> getHandlers() {
        return new ArrayList<>(handlers);
    }
    
    // Getters and setters
    public String getName() {
        return name;
    }
    
    public void setLevel(LogLevel level) {
        this.level = level;
    }
    
    public LogLevel getLevel() {
        return level;
    }
    
    public void setParent(Logger parent) {
        this.parent = parent;
    }
    
    public Logger getParent() {
        return parent;
    }
    
    public void setUseParentHandlers(boolean useParentHandlers) {
        this.useParentHandlers = useParentHandlers;
    }
    
    // Database handler specific methods
    public void addDatabaseHandler(String databaseName) throws SQLException {
        DatabaseHandler dbHandler = new DatabaseHandler(databaseName);
        addHandler(dbHandler);
    }
    
    public void addDatabaseHandler(DatabaseHandler dbHandler) {
        addHandler(dbHandler);
    }
    
    public DatabaseHandler getDatabaseHandler() {
        for (Handler handler : handlers) {
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
    
    public boolean hasDatabaseHandler() {
        return getDatabaseHandler() != null;
    }
}