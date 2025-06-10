package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.Log;
import io.joshuasalcedo.logging.LogLevel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DatabaseHandlerTest {
    private DatabaseHandler handler;
    private LogRepository repository;
    
    @BeforeEach
    public void setUp() throws SQLException {
        // Use unique database name for each test to avoid conflicts
        String dbName = "test-logs-" + System.currentTimeMillis();
        handler = new DatabaseHandler(dbName);
        repository = handler.getRepository();
        // Clear any existing data
        repository.deleteAll();
    }
    
    @AfterEach
    public void tearDown() throws SQLException {
        if (repository != null) {
            repository.deleteAll();
        }
        if (handler != null) {
            handler.close();
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
}