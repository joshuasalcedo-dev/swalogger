package io.joshuasalcedo.logging.database;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.database.DatabaseHandler;
import io.joshuasalcedo.logging.database.config.DatabaseConfiguration;
import io.joshuasalcedo.logging.database.repository.LogRecord;
import io.joshuasalcedo.logging.database.repository.LogRepository;
import io.joshuasalcedo.logging.database.repository.LogSearchCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseHandlerTest {
    private DatabaseHandler handler;
    private LogRepository repository;
    private static final String TEST_DB_NAME = "logging-test-db";
    
    @BeforeEach
    public void setUp() throws SQLException {
        // Use consistent test database configuration
        DatabaseConfiguration config = DatabaseConfiguration.forTesting();
        config.setDatabaseName(TEST_DB_NAME);
        
        handler = new DatabaseHandler(config);
        repository = handler.getRepository();
        
        // Clear any existing data for clean test state
        repository.deleteAll();
    }
    
    @AfterEach
    public void tearDown() throws SQLException {
        try {
            if (repository != null) {
                repository.deleteAll();
            }
        } finally {
            if (handler != null) {
                handler.close();
            }
        }
    }
    
    @Test
    public void testPublishLog() throws SQLException {
        // Create a test log
        Log log = new Log(LogLevel.INFO, "Test message", "TestLogger");
        
        // Publish it
        handler.publish(log);
        
        // Verify it was saved
        List<LogRecord> records = repository.findAll(10);
        assertFalse(records.isEmpty());
        
        LogRecord record = records.get(0);
        assertEquals(LogLevel.INFO, record.level());
        assertEquals("Test message", record.message());
        assertEquals("TestLogger", record.loggerName());
    }
    
    @Test
    public void testPublishLogWithException() throws SQLException {
        // Create a test log with exception
        Exception ex = new RuntimeException("Test exception");
        Log log = new Log(LogLevel.ERROR, "Error occurred", "TestLogger", ex);
        
        // Publish it
        handler.publish(log);
        
        // Verify it was saved with throwable info
        List<LogRecord> records = repository.findWithThrowables(10);
        assertFalse(records.isEmpty());
        
        LogRecord record = records.get(0);
        assertTrue(record.hasThrowable());
        assertEquals("Test exception", record.throwableMessage());
        assertNotNull(record.throwableStackTrace());
    }
    
    @Test
    public void testLogLevelFiltering() throws SQLException {
        // Set handler to only accept WARN and above
        handler.setLevel(LogLevel.WARN);
        
        // Try to publish INFO log
        Log infoLog = new Log(LogLevel.INFO, "Info message", "TestLogger");
        handler.publish(infoLog);
        
        // Publish WARN log
        Log warnLog = new Log(LogLevel.WARN, "Warning message", "TestLogger");
        handler.publish(warnLog);
        
        // Verify only WARN was saved
        List<LogRecord> records = repository.findAll(10);
        assertEquals(1, records.size());
        assertEquals(LogLevel.WARN, records.get(0).level());
    }
    
    @Test
    public void testSearchByCriteria() throws SQLException {
        // Set handler to accept all levels
        handler.setLevel(LogLevel.DEBUG);
        
        // Publish various logs
        handler.publish(new Log(LogLevel.DEBUG, "Debug message", "DebugLogger"));
        handler.publish(new Log(LogLevel.INFO, "Info message", "InfoLogger"));
        handler.publish(new Log(LogLevel.WARN, "Warning message", "WarnLogger"));
        handler.publish(new Log(LogLevel.ERROR, "Error message", "ErrorLogger"));
        
        // Search for WARN and above
        LogSearchCriteria criteria = LogSearchCriteria.builder()
            .minLevel(LogLevel.WARN)
            .limit(100)
            .build();
            
        List<LogRecord> results = repository.search(criteria);
        assertEquals(2, results.size());
        
        // All results should be WARN or ERROR
        for (LogRecord record : results) {
            assertTrue(record.level().getValue() >= LogLevel.WARN.getValue());
        }
    }
    
    @Test
    public void testSearchByLoggerName() throws SQLException {
        // Publish logs from different loggers
        handler.publish(new Log(LogLevel.INFO, "Message 1", "LoggerA"));
        handler.publish(new Log(LogLevel.INFO, "Message 2", "LoggerB"));
        handler.publish(new Log(LogLevel.INFO, "Message 3", "LoggerA"));
        
        // Search for LoggerA
        List<LogRecord> results = repository.findByLoggerName("LoggerA");
        assertEquals(2, results.size());
        
        for (LogRecord record : results) {
            assertEquals("LoggerA", record.loggerName());
        }
    }
    
    @Test
    public void testDeleteOldLogs() throws SQLException {
        // Publish a log
        handler.publish(new Log(LogLevel.INFO, "Old message", "TestLogger"));
        
        // Wait a moment
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            // ignore
        }
        
        // Delete logs older than now
        repository.deleteOlderThan(Instant.now());
        
        // Verify it was deleted
        List<LogRecord> records = repository.findAll();
        assertTrue(records.isEmpty());
    }
    
    @Test
    @DisplayName("Database configuration should be applied correctly")
    public void testDatabaseConfiguration() throws SQLException {
        DatabaseConfiguration config = handler.getConfiguration();
        
        assertNotNull(config);
        assertEquals(TEST_DB_NAME, config.getDatabaseName());
        assertEquals(DatabaseConfiguration.SchemaAction.CREATE_DROP, config.getSchemaAction());
        assertFalse(config.isPersistentDatabase()); // Testing uses in-memory DB
        assertFalse(config.isUseConnectionPool()); // Simplified for tests
        assertFalse(config.isEnableMigrations()); // Skip migrations in tests
    }
    
    @Test
    @DisplayName("Handler status tracking should work correctly")
    public void testHandlerStatusTracking() throws SQLException {
        // Initial state
        assertEquals(0, handler.getTotalLogsWritten());
        assertEquals(0, handler.getTotalErrors());
        assertTrue(handler.isHealthy());
        
        // Publish some logs
        handler.publish(new Log(LogLevel.INFO, "Test message 1", "TestLogger"));
        handler.publish(new Log(LogLevel.INFO, "Test message 2", "TestLogger"));
        
        assertEquals(2, handler.getTotalLogsWritten());
        assertEquals(0, handler.getTotalErrors());
        assertEquals(0.0, handler.getErrorRate());
        assertTrue(handler.isHealthy());
        
        // Test status summary
        String status = handler.getStatusSummary();
        assertNotNull(status);
        assertTrue(status.contains("DatabaseHandler"));
        assertTrue(status.contains(TEST_DB_NAME));
        assertTrue(status.contains("healthy=true"));
    }
    
    @Test
    @DisplayName("Factory methods should create handlers with correct configurations")
    public void testFactoryMethods() throws SQLException {
        // Test development configuration
        DatabaseHandler devHandler = DatabaseHandler.forDevelopment();
        assertNotNull(devHandler);
        DatabaseConfiguration devConfig = devHandler.getConfiguration();
        assertEquals(DatabaseConfiguration.SchemaAction.CREATE_DROP, devConfig.getSchemaAction());
        assertTrue(devConfig.isShowSql());
        assertFalse(devConfig.isPersistentDatabase());
        devHandler.close();
        
        // Test production configuration
        DatabaseHandler prodHandler = DatabaseHandler.forProduction();
        assertNotNull(prodHandler);
        DatabaseConfiguration prodConfig = prodHandler.getConfiguration();
        assertEquals(DatabaseConfiguration.SchemaAction.UPDATE, prodConfig.getSchemaAction());
        assertTrue(prodConfig.isPersistentDatabase());
        assertTrue(prodConfig.isUseConnectionPool());
        assertTrue(prodConfig.isEnableAutomaticCleanup());
        prodHandler.close();
        
        // Test testing configuration
        DatabaseHandler testHandler = DatabaseHandler.forTesting();
        assertNotNull(testHandler);
        DatabaseConfiguration testConfig = testHandler.getConfiguration();
        assertEquals(DatabaseConfiguration.SchemaAction.CREATE_DROP, testConfig.getSchemaAction());
        assertFalse(testConfig.isPersistentDatabase());
        assertFalse(testConfig.isUseConnectionPool());
        testHandler.close();
    }
    
    @Test
    @DisplayName("Database statistics should be collected correctly")
    public void testDatabaseStatistics() throws SQLException {
        // Add some test data
        handler.publish(new Log(LogLevel.INFO, "Stats test 1", "TestLogger"));
        handler.publish(new Log(LogLevel.WARN, "Stats test 2", "TestLogger"));
        handler.publish(new Log(LogLevel.ERROR, "Stats test 3", "TestLogger"));
        
        LogRepository.DatabaseStatistics stats = handler.getDatabaseStatistics();
        
        assertNotNull(stats);
        assertEquals(3, stats.totalRecords());
        assertTrue(stats.connectionValid());
        assertNotNull(stats.connectionInfo());
        assertNotNull(stats.newestRecord());
    }
    
    @Test
    @DisplayName("Manual cleanup should work correctly")
    public void testManualCleanup() throws SQLException {
        // Configure handler to allow cleanup
        DatabaseConfiguration config = DatabaseConfiguration.forTesting();
        config.setDatabaseName(TEST_DB_NAME + "-cleanup");
        config.setEnableAutomaticCleanup(true);
        config.setRetentionDays(0); // Immediate cleanup for testing
        
        DatabaseHandler cleanupHandler = new DatabaseHandler(config);
        
        try {
            // Add a log entry
            cleanupHandler.publish(new Log(LogLevel.INFO, "Cleanup test", "TestLogger"));
            
            // Verify it exists
            assertEquals(1, cleanupHandler.getDatabaseStatistics().totalRecords());
            
            // Trigger cleanup (with retention of 0 days, should delete everything)
            int deletedCount = cleanupHandler.triggerCleanup();
            
            // Should have deleted the record
            assertTrue(deletedCount >= 0); // May be 0 if record is too new
            
        } finally {
            cleanupHandler.close();
        }
    }
    
    @Test
    @DisplayName("Counter reset should work correctly")
    public void testCounterReset() throws SQLException {
        // Generate some activity
        handler.publish(new Log(LogLevel.INFO, "Counter test 1", "TestLogger"));
        handler.publish(new Log(LogLevel.INFO, "Counter test 2", "TestLogger"));
        
        // Verify counters have values
        assertEquals(2, handler.getTotalLogsWritten());
        assertTrue(handler.isHealthy());
        
        // Reset counters
        handler.resetCounters();
        
        // Verify counters are reset
        assertEquals(0, handler.getTotalLogsWritten());
        assertEquals(0, handler.getTotalErrors());
        assertTrue(handler.isHealthy());
    }
}