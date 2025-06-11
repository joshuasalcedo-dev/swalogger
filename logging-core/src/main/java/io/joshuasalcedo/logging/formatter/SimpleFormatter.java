package io.joshuasalcedo.logging.formatter;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class SimpleFormatter implements Formatter {
    private static final String FORMAT = "[%d][%s]-[%s]  %s - %s";
    private static final String DATE_FORMAT = "yyyy-MM-dd hh:mm:ss.SSS a";

    // ANSI Color Constants
    private static final String RESET = "\u001b[0m";
    private static final String DIM = "\u001b[90m";
    private static final String RED = "\u001b[31m";
    private static final String GREEN = "\u001b[32m";
    private static final String YELLOW = "\u001b[33m";
    private static final String CYAN = "\u001b[36m";
    private static final String WHITE = "\u001b[37m";
    private static final String BRIGHT_RED = "\u001b[91m";
    private static final String RED_BG_WHITE = "\u001b[41;37m";

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(DATE_FORMAT);

    // Method to get color based on log level
    private static String getLogLevelColor(LogLevel logLevel) {
        return switch (logLevel) {
            case CRITICAL -> RED_BG_WHITE;
            case ERROR -> BRIGHT_RED;
            case WARN, TODO -> YELLOW;
            case EVENT -> GREEN;
            case INFO -> CYAN;
            case DEBUG -> DIM;
        };
    }

    // Format log with colors
    @Override
    public String format(Log record) {
        String color = getLogLevelColor(record.getLevel());
        // Convert Instant to LocalDateTime for formatting
        LocalDateTime dateTime = LocalDateTime.ofInstant(record.getTimestamp(), ZoneId.systemDefault());
        String formattedDateTime = DATE_TIME_FORMATTER.format(dateTime);

        String formatted = String.format(
                "[%s%s%s]-[%s%s%s]  %s%s%s.%s%s%s() - %s%s%s",
                color, record.getLevel(), RESET,          // Colored log level
                CYAN, formattedDateTime, RESET,           // Cyan timestamp
                WHITE, record.getClassName(), RESET,      // White class name
                YELLOW, record.getMethodName(), RESET,    // Yellow method name
                color, record.getMessage(), RESET         // Colored message matching log level
        );

        // Add exception stack trace if present
        if (record.getThrowable() != null) {
            StringBuilder sb = new StringBuilder(formatted);
            sb.append("\n").append(formatStackTrace(record.getThrowable()));
            return sb.toString();
        }

        return formatted;
    }

    // Helper method to format stack trace with colors
    private static String formatStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();

        // Exception header with red color
        sb.append(BRIGHT_RED)
                .append("Exception: ")
                .append(throwable.getClass().getSimpleName())
                .append(": ")
                .append(throwable.getMessage() != null ? throwable.getMessage() : "No message")
                .append(RESET)
                .append("\n");

        // Stack trace elements
        StackTraceElement[] elements = throwable.getStackTrace();
        for (int i = 0; i < elements.length; i++) {
            StackTraceElement element = elements[i];

            // Color the first few stack frames differently (more relevant)
            String frameColor = i < 3 ? RED : DIM;

            sb.append(frameColor)
                    .append("    at ")
                    .append(WHITE)
                    .append(element.getClassName())
                    .append(".")
                    .append(YELLOW)
                    .append(element.getMethodName())
                    .append(WHITE)
                    .append("(")
                    .append(CYAN)
                    .append(element.getFileName() != null ? element.getFileName() : "Unknown Source")
                    .append(":")
                    .append(element.getLineNumber() > 0 ? element.getLineNumber() : "?")
                    .append(WHITE)
                    .append(")")
                    .append(RESET);

            if (i < elements.length - 1) {
                sb.append("\n");
            }
        }

        // Add caused by chain if present
        Throwable cause = throwable.getCause();
        if (cause != null) {
            sb.append("\n")
                    .append(YELLOW)
                    .append("Caused by: ")
                    .append(RESET)
                    .append(formatStackTrace(cause));
        }

        return sb.toString();
    }
}