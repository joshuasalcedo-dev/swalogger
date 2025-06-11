package io.joshuasalcedo.logging.database;

import io.joshuasalcedo.logging.database.repository.LogRepository;
import io.joshuasalcedo.logging.database.config.DatabaseConfiguration;
import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.formatter.Formatter;
import io.joshuasalcedo.logging.formatter.SimpleFormatter;
import io.joshuasalcedo.logging.handler.Handler;

import java.sql.SQLException;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Database handler with comprehensive configuration support and lifecycle management
 */
public class DatabaseHandler implements Handler {
    private Formatter formatter = new SimpleFormatter();
    private LogLevel level = LogLevel.INFO;
    private final LogRepository logRepository;
    private final DatabaseConfiguration config;
    private final AtomicLong totalLogsWritten = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);
    private volatile boolean healthy = true;
    private ScheduledExecutorService cleanupExecutor;
    
    // Constructors
    public DatabaseHandler(DatabaseConfiguration config) throws SQLException {
        this.config = config;
        this.logRepository = new LogRepository(config);
        initializeCleanupScheduler();
    }
    
    public DatabaseHandler(String databaseName) throws SQLException {
        this.config = new DatabaseConfiguration();
        config.setDatabaseName(databaseName);
        config.setPersistentDatabase(true);
        config.setSchemaAction(DatabaseConfiguration.SchemaAction.UPDATE);
        
        this.logRepository = new LogRepository(config);
        initializeCleanupScheduler();
    }
    
    public DatabaseHandler() throws SQLException {
        this(DatabaseConfiguration.forProduction());
    }
    
    // Legacy constructor for backward compatibility
    public DatabaseHandler(LogRepository logRepository) {
        this.logRepository = logRepository;
        this.config = logRepository.getConfiguration();
        initializeCleanupScheduler();
    }
    
    private void initializeCleanupScheduler() {
        if (config.isEnableAutomaticCleanup()) {
            this.cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "DatabaseHandler-Cleanup");
                t.setDaemon(true);
                return t;
            });
            
            // Schedule cleanup every hour (simplified from cron expression)
            cleanupExecutor.scheduleAtFixedRate(this::performCleanup, 1, 1, TimeUnit.HOURS);
        }
    }
    
    @Override
    public synchronized void publish(Log record) {
        if (record.getLevel().getValue() < level.getValue()) {
            return;
        }
        
        try {
            logRepository.save(record);
            totalLogsWritten.incrementAndGet();
            
            // Reset healthy status if we were previously unhealthy
            if (!healthy) {
                healthy = true;
            }
            
        } catch (SQLException e) {
            totalErrors.incrementAndGet();
            healthy = false;
            
            // Log error with context
            String errorMsg = String.format(
                "Failed to save log to database [%s]: %s. Total errors: %d",
                config.getDatabaseName(), e.getMessage(), totalErrors.get()
            );
            System.err.println(errorMsg);
            
            // Fallback to stderr if database fails
            System.err.println("Fallback log: " + formatter.format(record));
            
            // Optionally try to reconnect on certain errors
            if (isConnectionError(e)) {
                tryReconnect();
            }
        }
    }
    
    private boolean isConnectionError(SQLException e) {
        String sqlState = e.getSQLState();
        int errorCode = e.getErrorCode();
        
        // Common connection error indicators
        return sqlState != null && (
            sqlState.startsWith("08") ||  // Connection exception
            sqlState.startsWith("28") ||  // Invalid authorization
            errorCode == 90067 ||         // H2: Connection is broken
            errorCode == 90121            // H2: Database is already closed
        );
    }
    
    private void tryReconnect() {
        try {
            if (!logRepository.isConnectionValid()) {
                // Connection is invalid, but we can't easily recreate it with current architecture
                // Log the issue and mark as unhealthy
                System.err.println("Database connection is invalid. Manual intervention may be required.");
                healthy = false;
            }
        } catch (Exception e) {
            System.err.println("Failed to check connection validity: " + e.getMessage());
            healthy = false;
        }
    }
    
    private void performCleanup() {
        try {
            int deletedRows = logRepository.performCleanup();
            if (deletedRows > 0 && config.isShowSql()) {
                System.out.println("DatabaseHandler: Cleaned up " + deletedRows + " old log records");
            }
        } catch (SQLException e) {
            totalErrors.incrementAndGet();
            System.err.println("Failed to perform automatic cleanup: " + e.getMessage());
        }
    }
    
    @Override
    public void setFormatter(Formatter formatter) {
        this.formatter = formatter != null ? formatter : new SimpleFormatter();
    }
    
    @Override
    public void setLevel(LogLevel level) {
        this.level = level != null ? level : LogLevel.INFO;
    }
    
    @Override
    public LogLevel getLevel() {
        return level;
    }
    
    @Override
    public void close() {
        try {
            // Shutdown cleanup scheduler
            if (cleanupExecutor != null && !cleanupExecutor.isShutdown()) {
                cleanupExecutor.shutdown();
                try {
                    if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        cleanupExecutor.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    cleanupExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            
            // Close repository
            logRepository.close();
            
        } catch (SQLException e) {
            System.err.println("Failed to close database handler: " + e.getMessage());
        }
    }
    
    // Getters and status methods
    public LogRepository getRepository() {
        return logRepository;
    }
    
    public DatabaseConfiguration getConfiguration() {
        return config;
    }
    
    public long getTotalLogsWritten() {
        return totalLogsWritten.get();
    }
    
    public long getTotalErrors() {
        return totalErrors.get();
    }
    
    public boolean isHealthy() {
        return healthy && logRepository.isConnectionValid();
    }
    
    public double getErrorRate() {
        long total = totalLogsWritten.get();
        if (total == 0) return 0.0;
        return (double) totalErrors.get() / total * 100.0;
    }
    
    public String getStatusSummary() {
        try {
            LogRepository.DatabaseStatistics stats = logRepository.getStatistics();
            return String.format(
                "DatabaseHandler[%s]: %d logs written, %d errors (%.2f%%), healthy=%s, %s",
                config.getDatabaseName(),
                totalLogsWritten.get(),
                totalErrors.get(),
                getErrorRate(),
                isHealthy(),
                stats.toString()
            );
        } catch (SQLException e) {
            return String.format(
                "DatabaseHandler[%s]: %d logs written, %d errors, healthy=%s, stats unavailable: %s",
                config.getDatabaseName(),
                totalLogsWritten.get(),
                totalErrors.get(),
                isHealthy(),
                e.getMessage()
            );
        }
    }
    
    /**
     * Manually trigger cleanup of old records
     * @return number of records deleted
     */
    public int triggerCleanup() throws SQLException {
        return logRepository.performCleanup();
    }
    
    /**
     * Get database statistics
     */
    public LogRepository.DatabaseStatistics getDatabaseStatistics() throws SQLException {
        return logRepository.getStatistics();
    }
    
    /**
     * Reset error counters
     */
    public void resetCounters() {
        totalLogsWritten.set(0);
        totalErrors.set(0);
        healthy = true;
    }
    
    /**
     * Factory methods for common configurations
     */
    public static DatabaseHandler forDevelopment() throws SQLException {
        return new DatabaseHandler(DatabaseConfiguration.forDevelopment());
    }
    
    public static DatabaseHandler forProduction() throws SQLException {
        return new DatabaseHandler(DatabaseConfiguration.forProduction());
    }
    
    public static DatabaseHandler forTesting() throws SQLException {
        return new DatabaseHandler(DatabaseConfiguration.forTesting());
    }
}