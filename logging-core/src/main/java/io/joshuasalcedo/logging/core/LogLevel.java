package io.joshuasalcedo.logging.core;

import lombok.Getter;

@SuppressWarnings("unused")
public enum LogLevel {
    DEBUG(10),      // Detailed diagnostic information
    TODO(15),       // Development markers (if you want them only in dev)
    INFO(20),       // General information
    EVENT(30),      // Important business events
    WARN(40),    // Warning conditions
    ERROR(50),      // Error conditions
    CRITICAL(60);   // System-critical failures

    private final int value;

    LogLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
