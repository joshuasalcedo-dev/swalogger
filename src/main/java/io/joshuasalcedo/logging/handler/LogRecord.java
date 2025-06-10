package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.LogLevel;

import java.time.Instant;
import java.time.LocalDateTime;

public record LogRecord(
    Long id,
    LogLevel level,
    String message,
    String loggerName,
    Instant timestamp,
    String className,
    String methodName,
    int lineNumber,
    String throwableMessage,
    String throwableStackTrace,
    LocalDateTime createdAt
) {
    public boolean hasThrowable() {
        return throwableMessage != null && !throwableMessage.trim().isEmpty();
    }
}