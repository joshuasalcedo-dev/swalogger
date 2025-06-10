package io.joshuasalcedo.logging;

import io.joshuasalcedo.logging.handler.Formatter;

/**
 * Structured formatter for key-value logging
 */
public class StructuredFormatter implements Formatter {
    private final String separator;
    
    public StructuredFormatter() {
        this(" | ");
    }
    
    public StructuredFormatter(String separator) {
        this.separator = separator;
    }
    
    @Override
    public String format(Log record) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("timestamp=").append(record.getTimestamp());
        sb.append(separator).append("level=").append(record.getLevel());
        sb.append(separator).append("logger=").append(record.getLoggerName());
        sb.append(separator).append("class=").append(record.getClassName());
        sb.append(separator).append("method=").append(record.getMethodName());
        sb.append(separator).append("line=").append(record.getLineNumber());
        sb.append(separator).append("message=\"").append(record.getMessage()).append("\"");
        
        if (record.getThrowable() != null) {
            sb.append(separator).append("exception=").append(record.getThrowable().getClass().getSimpleName());
            sb.append(separator).append("exceptionMessage=\"").append(record.getThrowable().getMessage()).append("\"");
        }
        
        return sb.toString();
    }
}