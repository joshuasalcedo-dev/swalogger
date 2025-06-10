package io.joshuasalcedo.logging;

import io.joshuasalcedo.logging.handler.Formatter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Pattern-based formatter similar to Logback
 */
public class PatternFormatter implements Formatter {
    private final String pattern;
    private final DateTimeFormatter dateFormatter;
    
    public PatternFormatter(String pattern) {
        this.pattern = pattern;
        this.dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    }
    
    @Override
    public String format(Log record) {
        String result = pattern;
        
        result = result.replace("%d", dateFormatter.format(
            LocalDateTime.ofInstant(record.getTimestamp(), ZoneId.systemDefault())));
        result = result.replace("%level", record.getLevel().name());
        result = result.replace("%logger", record.getLoggerName());
        result = result.replace("%class", record.getClassName());
        result = result.replace("%method", record.getMethodName());
        result = result.replace("%line", String.valueOf(record.getLineNumber()));
        result = result.replace("%message", record.getMessage());
        result = result.replace("%n", System.lineSeparator());
        
        if (record.getThrowable() != null && result.contains("%exception")) {
            result = result.replace("%exception", getStackTraceAsString(record.getThrowable()));
        } else {
            result = result.replace("%exception", "");
        }
        
        return result;
    }
    
    private String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}