package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.Log;

public interface Formatter {
    String format(Log record);
}