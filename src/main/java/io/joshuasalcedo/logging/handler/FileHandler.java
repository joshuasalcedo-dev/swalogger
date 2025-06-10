package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.Log;
import io.joshuasalcedo.logging.LogLevel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class FileHandler implements Handler {
    private Formatter formatter = new LogFormatter();
    private LogLevel level = LogLevel.INFO;
    private PrintWriter writer;
    private final String filename;
    
    public FileHandler(String filename) throws IOException {
        this.filename = filename;
        this.writer = new PrintWriter(new BufferedWriter(new FileWriter(filename, true)));
    }
    
    @Override
    public synchronized void publish(Log record) {
        if (record.getLevel().getValue() >= level.getValue()) {
            writer.println(formatter.format(record));
            writer.flush();
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
        if (writer != null) {
            writer.close();
        }
    }
}