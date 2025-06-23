package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.formatter.Formatter;
import io.joshuasalcedo.logging.formatter.SimpleFormatter;

import java.io.PrintStream;

public class ConsoleHandler implements Handler {
    private Formatter formatter = new SimpleFormatter();
    private LogLevel level = LogLevel.INFO;
    private final PrintStream stream;

    public ConsoleHandler() {
        this(System.out);
    }

    public ConsoleHandler(PrintStream stream) {
        this.stream = stream;
    }

    @Override
    public void publish(Log record) {
        if (record.getLevel().getValue() >= level.getValue()) {
            stream.println(formatter.format(record));
            stream.flush();
        }
    }

    @Override
    public void setFormatter(Formatter formatter) {
        this.formatter = formatter;
    }

    @Override
    public void setLevel(LogLevel level) {
        this.level = level;
    }

    @Override
    public LogLevel getLevel() {
        return level;
    }

    @Override
    public void close() {
        stream.flush();
    }
}