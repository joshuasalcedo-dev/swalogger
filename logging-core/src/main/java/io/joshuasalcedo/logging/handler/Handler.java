package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.formatter.Formatter;

public interface Handler {
    void publish(Log record);
    void setFormatter(Formatter formatter);
    void setLevel(LogLevel level);
    LogLevel getLevel();
    void close();
}