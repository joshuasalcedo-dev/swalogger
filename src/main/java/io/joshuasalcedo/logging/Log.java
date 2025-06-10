package io.joshuasalcedo.logging;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Builder
public class Log {
    private final LogLevel level;
    private final String message;
    private final String loggerName;
    private final Instant timestamp;
    private final String className;
    private final String methodName;
    private final int lineNumber;
    private Throwable throwable;

    public Log(LogLevel level, String message, String loggerName) {
        this.level = level;
        this.message = message;
        this.loggerName = loggerName;
        this.timestamp = Instant.now();

        // Get caller information from stack trace
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        // Skip getStackTrace() and this constructor
        StackTraceElement caller = findCaller(stackTrace);

        this.className = caller.getClassName();
        this.methodName = caller.getMethodName();
        this.lineNumber = caller.getLineNumber();
        this.throwable = null;
    }

    public Log(LogLevel level, String message, String loggerName, Throwable throwable) {
        this(level, message, loggerName);
        this.throwable = throwable;
    }

    private StackTraceElement findCaller(StackTraceElement[] stackTrace) {
        // Skip internal logging framework calls
        for (int i = 3; i < stackTrace.length; i++) {
            if (!stackTrace[i].getClassName().startsWith("io.joshuasalcedo.logging")) {
                return stackTrace[i];
            }
        }
        return stackTrace[3]; // Fallback
    }


    public LogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public String getLoggerName() {
        return loggerName;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public int getLineNumber() {
        return lineNumber;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public void setThrowable(Throwable throwable) {
        this.throwable = throwable;
    }
}