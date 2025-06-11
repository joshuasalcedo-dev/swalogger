package io.joshuasalcedo.logging.util;

/**
 * Custom exceptions for the logging framework
 */
public class LoggingException extends RuntimeException {
    
    public LoggingException(String message) {
        super(message);
    }
    
    public LoggingException(String message, Throwable cause) {
        super(message, cause);
    }
}
