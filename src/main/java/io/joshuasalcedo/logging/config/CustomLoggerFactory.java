package io.joshuasalcedo.logging.config;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import java.util.concurrent.ConcurrentHashMap;

public class CustomLoggerFactory implements ILoggerFactory {
    private final ConcurrentHashMap<String, Logger> loggerMap = new ConcurrentHashMap<>();
    
    @Override
    public Logger getLogger(String name) {
        return loggerMap.computeIfAbsent(name, CustomLoggerAdapter::new);
    }
}