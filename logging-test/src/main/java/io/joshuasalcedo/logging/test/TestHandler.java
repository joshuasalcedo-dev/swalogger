package io.joshuasalcedo.logging.test;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.handler.Handler;
import io.joshuasalcedo.logging.formatter.Formatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Test handler that captures logs in memory for testing
 */
public class TestHandler implements Handler {
    
    private final List<Log> logs = new ArrayList<>();
    private Formatter formatter;
    private LogLevel level = LogLevel.DEBUG;
    
    @Override
    public void publish(Log record) {
        if (record.getLevel().getValue() >= level.getValue()) {
            logs.add(record);
        }
    }
    
    public List<Log> getLogs() {
        return new ArrayList<>(logs);
    }
    
    public void clear() {
        logs.clear();
    }
    
    public int getLogCount() {
        return logs.size();
    }
    
    public Log getLastLog() {
        return logs.isEmpty() ? null : logs.get(logs.size() - 1);
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
        logs.clear();
    }
}
