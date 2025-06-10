package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.Log;
import io.joshuasalcedo.logging.LogLevel;

import java.sql.SQLException;

public class DatabaseHandler implements Handler {
    private Formatter formatter = new LogFormatter();
    private LogLevel level = LogLevel.INFO;
    private final LogRepository logRepository;
    
    public DatabaseHandler() throws SQLException {
        this("logs");
    }
    
    public DatabaseHandler(String databaseName) throws SQLException {
        this.logRepository = new LogRepository(databaseName);
    }
    
    public DatabaseHandler(LogRepository logRepository) {
        this.logRepository = logRepository;
    }
    
    @Override
    public synchronized void publish(Log record) {
        if (record.getLevel().getValue() >= level.getValue()) {
            try {
                logRepository.save(record);
            } catch (SQLException e) {
                // Fallback to stderr if database fails
                System.err.println("Failed to persist log to database: " + e.getMessage());
                System.err.println(formatter.format(record));
            }
        }
    }
    
    @Override
    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }
    
    @Override
    public void setLevel(LogLevel level) {
        this.level = level;
    }
    
    @Override
    public LogLevel getLevel() {
        return level;
    }
    
    @Override
    public void close() {
        try {
            logRepository.close();
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
        }
    }
    
    public LogRepository getRepository() {
        return logRepository;
    }
}