package io.joshuasalcedo.logging.database.repository;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;

public class LogMapper {
    
    /**
     * Maps a Log entity to a LogRecord DTO
     */
    public static LogRecord toLogRecord(Log log) {
        return new LogRecord(
            null, // ID will be set after database insertion
            log.getLevel(),
            log.getMessage(),
            log.getLoggerName(),
            log.getTimestamp(),
            log.getClassName(),
            log.getMethodName(),
            log.getLineNumber(),
            log.getThrowable() != null ? log.getThrowable().getMessage() : null,
            log.getThrowable() != null ? getStackTraceAsString(log.getThrowable()) : null,
            null // createdAt will be set by database
        );
    }
    
    /**
     * Maps a database ResultSet row to a LogRecord DTO
     */
    public static LogRecord fromResultSet(ResultSet rs) throws SQLException {
        return new LogRecord(
            rs.getLong("id"),
            LogLevel.valueOf(rs.getString("level")),
            rs.getString("message"),
            rs.getString("logger_name"),
            rs.getTimestamp("timestamp").toInstant(),
            rs.getString("class_name"),
            rs.getString("method_name"),
            rs.getInt("line_number"),
            rs.getString("throwable_message"),
            rs.getString("throwable_stack_trace"),
            rs.getTimestamp("created_at").toLocalDateTime()
        );
    }
    
    /**
     * Creates a Log entity from a LogRecord (for cases where you need to recreate Log objects)
     * Note: This creates a new Log with current timestamp and stack trace
     */
    public static Log toLog(LogRecord logRecord) {
        if (logRecord.hasThrowable()) {
            // Create a runtime exception to represent the original throwable
            RuntimeException throwable = new RuntimeException(logRecord.throwableMessage());
            return new Log(logRecord.level(), logRecord.message(), logRecord.loggerName(), throwable);
        } else {
            return new Log(logRecord.level(), logRecord.message(), logRecord.loggerName());
        }
    }
    
    private static String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}