package io.joshuasalcedo.logging;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.joshuasalcedo.logging.handler.Formatter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.LocalDateTime;


/**
 * JSON formatter for structured logging
 */
public class JsonFormatter implements Formatter {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private final boolean includeStackTrace;
    
    public JsonFormatter() {
        this(true);
    }
    
    public JsonFormatter(boolean includeStackTrace) {
        this.includeStackTrace = includeStackTrace;
    }
    
    @Override
    public String format(Log record) {
        try {
            ObjectNode json = objectMapper.createObjectNode();
            
            json.put("timestamp", dateFormatter.format(
                LocalDateTime.ofInstant(record.getTimestamp(), ZoneId.systemDefault())));
            json.put("level", record.getLevel().name());
            json.put("logger", record.getLoggerName());
            json.put("message", record.getMessage());
            json.put("class", record.getClassName());
            json.put("method", record.getMethodName());
            json.put("line", record.getLineNumber());
            
            if (record.getThrowable() != null) {
                ObjectNode exception = json.putObject("exception");
                exception.put("type", record.getThrowable().getClass().getSimpleName());
                exception.put("message", record.getThrowable().getMessage());
                
                if (includeStackTrace) {
                    exception.put("stackTrace", getStackTraceAsString(record.getThrowable()));
                }
            }
            
            return objectMapper.writeValueAsString(json);
        } catch (Exception e) {
            // Fallback to simple format if JSON serialization fails
            return String.format("{\"error\":\"Failed to serialize log: %s\"}", e.getMessage());
        }
    }
    
    private String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}