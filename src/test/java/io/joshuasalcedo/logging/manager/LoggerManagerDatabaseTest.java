package io.joshuasalcedo.logging.manager;

import io.joshuasalcedo.logging.LogLevel;
import io.joshuasalcedo.logging.handler.DatabaseHandler;
import io.joshuasalcedo.logging.handler.LogRecord;
import io.joshuasalcedo.logging.handler.LogRepository;
import io.joshuasalcedo.logging.handler.LogSearchCriteria;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class LoggerManagerDatabaseTest {
    private String testDbName;
    
    @BeforeEach
    public void setUp() throws SQLException {
        // Clear any existing database logging
        LoggerManager.disableDatabaseLogging();
        // Generate unique database name for each test
        testDbName = "test-logger-manager-" + System.currentTimeMillis();
    }
    
    @AfterEach
    public void tearDown() throws SQLException {
        // Clean up database handler
        DatabaseHandler dbHandler = LoggerManager.getRootDatabaseHandler();
        if (dbHandler != null) {
            LogRepository repository = dbHandler.getRepository();
            repository.deleteAll();
        }
        LoggerManager.disableDatabaseLogging();
    }
    
    @Test
    public void testEnableDatabaseLogging() throws SQLException {
        // Enable database logging
        LoggerManager.enableDatabaseLogging(testDbName);
        
        // Get a logger and log some messages
        Logger logger = LoggerManager.getLogger("com.example.TestClass");
        logger.info("Test info message");
        logger.error("Test error message");
        
        // Verify database handler exists
        DatabaseHandler dbHandler = LoggerManager.getRootDatabaseHandler();
        assertNotNull(dbHandler);
        
        // Query the database
        LogRepository repository = dbHandler.getRepository();
        List<LogRecord> records = repository.findAll();
        
        // Should have 2 records
        assertEquals(2, records.size());
        
        // Verify the records
        LogRecord infoRecord = records.stream()
            .filter(r -> r.message().equals("Test info message"))
            .findFirst()
            .orElse(null);
        assertNotNull(infoRecord);
        assertEquals(LogLevel.INFO, infoRecord.level());
        assertEquals("com.example.TestClass", infoRecord.loggerName());
        
        LogRecord errorRecord = records.stream()
            .filter(r -> r.message().equals("Test error message"))
            .findFirst()
            .orElse(null);
        assertNotNull(errorRecord);
        assertEquals(LogLevel.ERROR, errorRecord.level());
    }
    
    @Test
    public void testDatabaseLoggingWithLevel() throws SQLException {
        // Enable database logging with WARN level
        LoggerManager.enableDatabaseLogging(testDbName, LogLevel.WARN);
        
        // Get a logger and log messages at different levels
        Logger logger = LoggerManager.getLogger("com.example.LevelTest");
        logger.debug("Debug message");
        logger.info("Info message");
        logger.warning("Warning message");
        logger.error("Error message");
        
        // Query the database
        DatabaseHandler dbHandler = LoggerManager.getRootDatabaseHandler();
        LogRepository repository = dbHandler.getRepository();
        List<LogRecord> records = repository.findAll();
        
        // Should only have WARN and ERROR messages
        assertEquals(2, records.size());
        assertTrue(records.stream().allMatch(r -> 
            r.level() == LogLevel.WARN || r.level() == LogLevel.ERROR));
    }
    
    @Test
    public void testLoggerSpecificDatabaseHandler() throws SQLException {
        // Get a specific logger
        Logger logger = LoggerManager.getLogger("com.example.SpecificLogger");
        
        // Add database handler to this specific logger only
        logger.addDatabaseHandler(testDbName);
        logger.setUseParentHandlers(false); // Don't propagate to parent
        
        // Log some messages
        logger.info("Specific logger message");
        
        // Log from another logger
        Logger otherLogger = LoggerManager.getLogger("com.example.OtherLogger");
        otherLogger.info("Other logger message");
        
        // Query the database
        DatabaseHandler dbHandler = logger.getDatabaseHandler();
        assertNotNull(dbHandler);
        LogRepository repository = dbHandler.getRepository();
        List<LogRecord> records = repository.findAll();
        
        // Should only have the specific logger's message
        assertEquals(1, records.size());
        assertEquals("Specific logger message", records.get(0).message());
        assertEquals("com.example.SpecificLogger", records.get(0).loggerName());
    }
    
    @Test
    public void testConfigureLogging() throws SQLException {
        // Configure both console and database logging
        LoggerManager.configureLogging(true, true, testDbName);
        
        // Get a logger and log messages
        Logger logger = LoggerManager.getLogger(LoggerManagerDatabaseTest.class);
        logger.info("Test from class logger");
        logger.error("Error from class logger", new RuntimeException("Test exception"));
        
        // Verify database handler exists
        DatabaseHandler dbHandler = LoggerManager.getRootDatabaseHandler();
        assertNotNull(dbHandler);
        
        // Query for logs with exceptions
        LogRepository repository = dbHandler.getRepository();
        List<LogRecord> recordsWithThrowables = repository.findWithThrowables();
        
        // Should have 1 record with throwable
        assertEquals(1, recordsWithThrowables.size());
        assertTrue(recordsWithThrowables.get(0).hasThrowable());
        assertEquals("Test exception", recordsWithThrowables.get(0).throwableMessage());
    }
    
    @Test
    public void testHierarchicalLogging() throws SQLException {
        // Enable database logging
        LoggerManager.enableDatabaseLogging(testDbName);
        
        // Create a hierarchy of loggers
        Logger parentLogger = LoggerManager.getLogger("com.example");
        Logger childLogger = LoggerManager.getLogger("com.example.child");
        Logger grandchildLogger = LoggerManager.getLogger("com.example.child.grandchild");
        
        // Set different levels
        parentLogger.setLevel(LogLevel.INFO);
        childLogger.setLevel(LogLevel.WARN);
        
        // Log from each logger
        parentLogger.info("Parent info");
        childLogger.info("Child info - should not appear");
        childLogger.warning("Child warning");
        grandchildLogger.error("Grandchild error");
        
        // Query the database
        DatabaseHandler dbHandler = LoggerManager.getRootDatabaseHandler();
        LogRepository repository = dbHandler.getRepository();
        
        // Search using criteria
        LogSearchCriteria criteria = LogSearchCriteria.builder()
            .minLevel(LogLevel.INFO)
            .build();
        
        List<LogRecord> records = repository.search(criteria);
        
        // Should have parent info, child warning, and grandchild error
        assertEquals(3, records.size());
        
        // Verify no "Child info" message
        assertTrue(records.stream().noneMatch(r -> r.message().equals("Child info - should not appear")));
    }
}