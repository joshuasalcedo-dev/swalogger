package io.joshuasalcedo.logging.config;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {
    private static final StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
    private static final String loggerFactoryClassStr = LoggingConfiguration.class.getName();
    private final ILoggerFactory loggerFactory = new CustomLoggerFactory();
    
    public static StaticLoggerBinder getSingleton() {
        return SINGLETON;
    }
    
    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }
    
    @Override
    public String getLoggerFactoryClassStr() {
        return loggerFactoryClassStr;
    }
}