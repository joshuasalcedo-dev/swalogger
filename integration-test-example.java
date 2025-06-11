// Integration Test Example for Joshua Salcedo Logging Framework
// This demonstrates the implemented features

import io.joshuasalcedo.logging.core.LoggingFacade;
import io.joshuasalcedo.logging.core.Logger;
import io.joshuasalcedo.logging.handler.JLineHandler;

public class IntegrationTestExample {
    public static void main(String[] args) {
        System.out.println("=== Joshua Salcedo Logging Framework Integration Test ===\n");
        
        // Test 1: Default Configuration
        System.out.println("1. Testing Default Configuration:");
        LoggingFacade.configure();
        System.out.println(LoggingFacade.getConfigurationStatus());
        
        Logger logger = LoggingFacade.getLogger();
        logger.info("This is a test message with default configuration");
        
        // Test 2: Development Setup with JLine (colored output)
        System.out.println("\n2. Testing Development Setup (JLine colored output):");
        LoggingFacade.setupDevelopmentLogging();
        
        Logger devLogger = LoggingFacade.getLogger("DevelopmentLogger");
        devLogger.debug("Debug message - should be cyan");
        devLogger.info("Info message - should be green");
        devLogger.warning("Warning message - should be bold yellow");
        devLogger.error("Error message - should be bold red");
        devLogger.critical("Critical message - should be bold magenta");
        
        // Test 3: JLine Color Test
        System.out.println("\n3. Testing JLine Color Output:");
        try {
            JLineHandler jlineHandler = new JLineHandler(true);
            jlineHandler.printColorTest();
            System.out.println("Terminal Type: " + jlineHandler.getTerminalType());
            System.out.println("Terminal Detected: " + jlineHandler.isTerminalDetected());
            System.out.println("Color Enabled: " + jlineHandler.isColorEnabled());
        } catch (Exception e) {
            System.out.println("JLine not available: " + e.getMessage());
        }
        
        // Test 4: Production Setup
        System.out.println("\n4. Testing Production Setup:");
        LoggingFacade.setupProductionLogging();
        System.out.println(LoggingFacade.getConfigurationStatus());
        
        Logger prodLogger = LoggingFacade.getLogger("ProductionLogger");
        prodLogger.info("Production info message");
        prodLogger.warning("Production warning message");
        prodLogger.error("Production error message");
        
        // Test 5: Configuration Status
        System.out.println("\n5. Final Configuration Status:");
        System.out.println(LoggingFacade.getConfigurationStatus());
        
        // Test 6: Exception Logging
        System.out.println("\n6. Testing Exception Logging:");
        try {
            throw new RuntimeException("Test exception for logging");
        } catch (Exception e) {
            logger.error("Caught an exception", e);
        }
        
        System.out.println("\n=== Integration Test Complete ===");
    }
}