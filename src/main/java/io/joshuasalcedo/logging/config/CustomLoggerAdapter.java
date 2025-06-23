package io.joshuasalcedo.logging.config;

import io.joshuasalcedo.logging.LogLevel;
import io.joshuasalcedo.logging.manager.Logger;
import io.joshuasalcedo.logging.manager.LoggerManager;
import org.slf4j.Marker;
import org.slf4j.helpers.LegacyAbstractLogger;
import org.slf4j.helpers.MessageFormatter;

/**
 * SLF4J adapter for your custom logging library
 */
public class CustomLoggerAdapter extends LegacyAbstractLogger {
    private final Logger logger;
    
    public CustomLoggerAdapter(String name) {
        this.name = name;
        this.logger = LoggerManager.getLogger(name);
    }
    
    @Override
    public boolean isTraceEnabled() {
        return logger.getLevel().getValue() <= LogLevel.DEBUG.getValue();
    }
    
    @Override
    public boolean isDebugEnabled() {
        return logger.getLevel().getValue() <= LogLevel.DEBUG.getValue();
    }
    
    @Override
    public boolean isInfoEnabled() {
        return logger.getLevel().getValue() <= LogLevel.INFO.getValue();
    }
    
    @Override
    public boolean isWarnEnabled() {
        return logger.getLevel().getValue() <= LogLevel.WARN.getValue();
    }
    
    @Override
    public boolean isErrorEnabled() {
        return logger.getLevel().getValue() <= LogLevel.ERROR.getValue();
    }
    
    @Override
    protected String getFullyQualifiedCallerName() {
        return CustomLoggerAdapter.class.getName();
    }
    
    @Override
    protected void handleNormalizedLoggingCall(org.slf4j.event.Level level, Marker marker,
                                             String messagePattern, Object[] arguments, Throwable throwable) {
        String formattedMessage = MessageFormatter.basicArrayFormat(messagePattern, arguments);
        LogLevel customLevel = mapSlf4jLevel(level);
        
        if (throwable != null) {
            logger.log(customLevel, formattedMessage, throwable);
        } else {
            logger.log(customLevel, formattedMessage);
        }
    }
    
    private LogLevel mapSlf4jLevel(org.slf4j.event.Level slf4jLevel) {
        return switch (slf4jLevel) {
            case TRACE, DEBUG -> LogLevel.DEBUG;
            case INFO -> LogLevel.INFO;
            case WARN -> LogLevel.WARN;
            case ERROR -> LogLevel.ERROR;
        };
    }
}