package io.joshuasalcedo.logging.handler;


import io.joshuasalcedo.logging.Log;
import io.joshuasalcedo.logging.LogLevel;

public interface Handler {
    void publish(Log record);
    void setFormatter(Formatter formatter);
    void setLevel(LogLevel level);
    LogLevel getLevel();
    void close();
}
