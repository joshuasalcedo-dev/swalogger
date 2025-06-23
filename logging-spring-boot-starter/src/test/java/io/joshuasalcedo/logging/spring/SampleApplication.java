package io.joshuasalcedo.logging.spring;

import io.joshuasalcedo.logging.core.Logger;
import io.joshuasalcedo.logging.manager.LoggerManager;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sample Spring Boot application demonstrating the logging framework autoconfiguration
 */
@SpringBootApplication
public class SampleApplication implements CommandLineRunner {
    
    private static final Logger logger = LoggerManager.getLogger(SampleApplication.class);
    
    public static void main(String[] args) {
        SpringApplication.run(SampleApplication.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        logger.info("Starting sample application");
        
        // Test different log levels
        logger.debug("Debug message - should be visible in development mode");
        logger.info("Info message");
        logger.warning("Warning message");
        logger.error("Error message");
        
        // Test structured logging

        // Test exception logging
        try {
            throw new RuntimeException("Sample exception for testing");
        } catch (Exception e) {
            logger.error("Exception occurred", e);
        }
        
        logger.info("Sample application completed");
    }
}