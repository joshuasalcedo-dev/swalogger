package io.joshuasalcedo.logging.formatter;

import io.joshuasalcedo.logging.core.Log;

public interface Formatter {
    String format(Log record);
}