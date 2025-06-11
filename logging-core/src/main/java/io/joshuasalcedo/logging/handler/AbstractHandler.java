package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.formatter.Formatter;
import io.joshuasalcedo.logging.formatter.SimpleFormatter;

/**
 * Base implementation for handlers
 */
public abstract class AbstractHandler implements Handler {
    
    protected Formatter formatter = new SimpleFormatter();
    protected LogLevel level = LogLevel.INFO;
    
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
    
    protected boolean isLoggable(Log record) {
        return record.getLevel().getValue() >= level.getValue();
    }
}
