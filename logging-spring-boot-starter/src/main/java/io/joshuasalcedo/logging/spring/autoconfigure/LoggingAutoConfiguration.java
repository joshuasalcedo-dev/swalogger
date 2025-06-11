package io.joshuasalcedo.logging.spring.autoconfigure;

import io.joshuasalcedo.logging.async.AsyncConfiguration;
import io.joshuasalcedo.logging.async.AsyncHandler;
import io.joshuasalcedo.logging.config.LoggingConfiguration;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.core.LoggingFacade;
import io.joshuasalcedo.logging.database.DatabaseHandler;
import io.joshuasalcedo.logging.database.config.DatabaseConfiguration;
import io.joshuasalcedo.logging.formatter.*;
import io.joshuasalcedo.logging.handler.ConsoleHandler;
import io.joshuasalcedo.logging.handler.FileHandler;
import io.joshuasalcedo.logging.handler.Handler;
import io.joshuasalcedo.logging.handler.JLineHandler;
import io.joshuasalcedo.logging.manager.LoggerManager;
import io.joshuasalcedo.logging.metrics.LoggingMetrics;
import io.joshuasalcedo.logging.metrics.config.MetricsConfiguration;
import io.joshuasalcedo.logging.spring.properties.LoggingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot AutoConfiguration for Joshua Salcedo Logging Framework
 */
@AutoConfiguration
@ConditionalOnClass(LoggingFacade.class)
@ConditionalOnProperty(
    prefix = "joshuasalcedo.logging",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true
)
@EnableConfigurationProperties(LoggingProperties.class)
@Import({LoggingActuatorConfiguration.class})
public class LoggingAutoConfiguration {
    
    private final LoggingProperties properties;
    
    public LoggingAutoConfiguration(LoggingProperties properties) {
        this.properties = properties;
    }
    
    @Bean
    @ConditionalOnMissingBean
    public LoggingConfiguration loggingConfiguration() throws IOException {
        if (properties.getConfigFile() != null) {
            return new LoggingConfiguration(properties.getConfigFile());
        }
        return new LoggingConfiguration();
    }
    
    @Bean
    public String configureLogging(LoggingConfiguration configuration, List<Handler> handlers) {
        // Configure root logger level
        LogLevel rootLevel = LogLevel.valueOf(properties.getLevel().toUpperCase());
        LoggerManager.setRootLevel(rootLevel);
        
        // Clear existing handlers and add all configured handlers
        for (Handler existingHandler : LoggerManager.getRootHandlers()) {
            LoggerManager.removeRootHandler(existingHandler);
        }
        
        for (Handler handler : handlers) {
            LoggerManager.addRootHandler(handler);
        }
        
        // Configure additional loggers
        for (Map.Entry<String, String> entry : properties.getLoggers().entrySet()) {
            String loggerName = entry.getKey();
            String level = entry.getValue();
            LoggerManager.getLogger(loggerName).setLevel(LogLevel.valueOf(level.toUpperCase()));
        }
        
        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(LoggerManager::shutdown));
        
        return "LoggingConfigured";
    }
    
    @Configuration
    @ConditionalOnProperty(
        prefix = "joshuasalcedo.logging.console",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    static class ConsoleHandlerConfiguration {
        
        @Bean
        @ConditionalOnMissingBean(name = "consoleHandler")
        public Handler consoleHandler(LoggingProperties properties) {
            LoggingProperties.ConsoleProperties consoleProps = properties.getConsole();
            
            Handler handler;
            if (consoleProps.isUseJline()) {
                handler = new JLineHandler();
            } else {
                handler = new ConsoleHandler();
            }
            
            // Configure formatter
            Formatter formatter = createFormatter(
                consoleProps.getFormatter(),
                consoleProps.getPattern()
            );
            handler.setFormatter(formatter);
            
            return handler;
        }
    }
    
    @Configuration
    @ConditionalOnProperty(
        prefix = "joshuasalcedo.logging.file",
        name = "enabled",
        havingValue = "true"
    )
    static class FileHandlerConfiguration {
        
        @Bean
        @ConditionalOnMissingBean(name = "fileHandler")
        public Handler fileHandler(LoggingProperties properties) throws IOException {
            LoggingProperties.FileProperties fileProps = properties.getFile();
            
            // FileHandler constructor only takes filename, append is always true
            FileHandler handler = new FileHandler(fileProps.getPath());
            
            // Configure formatter
            Formatter formatter = createFormatter(
                fileProps.getFormatter(),
                fileProps.getPattern()
            );
            handler.setFormatter(formatter);
            
            return handler;
        }
    }
    
    @Configuration
    @ConditionalOnClass({DatabaseHandler.class, DataSource.class})
    @ConditionalOnProperty(
        prefix = "joshuasalcedo.logging.database",
        name = "enabled",
        havingValue = "true"
    )
    static class DatabaseHandlerConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public DatabaseConfiguration databaseConfiguration(LoggingProperties properties) {
            LoggingProperties.DatabaseProperties dbProps = properties.getDatabase();
            
            DatabaseConfiguration config = new DatabaseConfiguration();
            config.setUrl(dbProps.getUrl());
            config.setUsername(dbProps.getUsername());
            config.setPassword(dbProps.getPassword());
            config.setDriverClassName(dbProps.getDriverClassName());
            config.setCreateTablesAutomatically(dbProps.isCreateTables());
            
            return config;
        }
        
        @Bean
        @ConditionalOnMissingBean(name = "databaseHandler")
        public Handler databaseHandler(DatabaseConfiguration databaseConfig) throws java.sql.SQLException {
            return new DatabaseHandler(databaseConfig);
        }
    }
    
    @Configuration
    @ConditionalOnClass(AsyncHandler.class)
    @ConditionalOnProperty(
        prefix = "joshuasalcedo.logging.async",
        name = "enabled",
        havingValue = "true"
    )
    static class AsyncHandlerConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public AsyncConfiguration asyncConfiguration(LoggingProperties properties) {
            LoggingProperties.AsyncProperties asyncProps = properties.getAsync();
            
            AsyncConfiguration config = new AsyncConfiguration();
            config.setQueueSize(asyncProps.getQueueSize());
            config.setThreadCount(asyncProps.getWorkerThreads());
            config.setDiscardOnOverflow(asyncProps.isDropOnOverflow());
            config.setShutdownTimeoutSeconds(asyncProps.getShutdownTimeout() / 1000);
            
            return config;
        }
        
        @Bean
        @ConditionalOnMissingBean(name = "asyncHandler")
        public Handler asyncHandler(AsyncConfiguration asyncConfig, List<Handler> handlers) {
            // Find non-async handlers to wrap
            List<Handler> targetHandlers = new ArrayList<>();
            for (Handler handler : handlers) {
                if (!(handler instanceof AsyncHandler)) {
                    targetHandlers.add(handler);
                }
            }
            
            if (!targetHandlers.isEmpty()) {
                return new AsyncHandler(targetHandlers.get(0), asyncConfig.getQueueSize());
            }
            
            // Default to console handler if no other handlers
            return new AsyncHandler(new ConsoleHandler(), asyncConfig.getQueueSize());
        }
    }
    
    @Configuration
    @ConditionalOnClass(LoggingMetrics.class)
    @ConditionalOnProperty(
        prefix = "joshuasalcedo.logging.metrics",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
    )
    static class MetricsConfiguration {
        
        @Bean
        @ConditionalOnMissingBean
        public io.joshuasalcedo.logging.metrics.config.MetricsConfiguration metricsConfiguration(LoggingProperties properties) {
            LoggingProperties.MetricsProperties metricsProps = properties.getMetrics();
            
            io.joshuasalcedo.logging.metrics.config.MetricsConfiguration config = new io.joshuasalcedo.logging.metrics.config.MetricsConfiguration();
            config.setEnabled(metricsProps.isEnabled());
            config.setAutoReportEnabled(metricsProps.isAutoReport());
            config.setAutoReportIntervalMinutes(metricsProps.getAutoReportInterval());
            config.setExportEnabled(metricsProps.isExport());
            config.setExportFormat(metricsProps.getExportFormat());
            config.setExportPath(metricsProps.getExportPath());
            
            return config;
        }
        
        @Bean
        @ConditionalOnMissingBean
        public LoggingMetrics loggingMetrics(io.joshuasalcedo.logging.metrics.config.MetricsConfiguration metricsConfig) {
            LoggingMetrics metrics = LoggingMetrics.getInstance();
            // MetricsConfiguration is passed but LoggingMetrics doesn't have a configure method
            // Just return the singleton instance
            return metrics;
        }
    }
    
    /**
     * Helper method to create appropriate formatter based on type
     */
    private static Formatter createFormatter(String type, String pattern) {
        switch (type.toLowerCase()) {
            case "simple":
                return new SimpleFormatter();
            case "json":
                return new JsonFormatter();
            case "structured":
                return new StructuredFormatter();
            case "pattern":
            default:
                return new PatternFormatter(pattern);
        }
    }
    
    /**
     * Configuration class for production environments
     */
    @Configuration
    @ConditionalOnProperty(
        prefix = "joshuasalcedo.logging",
        name = "profile",
        havingValue = "production"
    )
    static class ProductionConfiguration {
        
        public ProductionConfiguration() {
            // Set production-specific settings
            LoggerManager.setRootLevel(LogLevel.INFO);
            
            // Disable debug logging for framework classes
            LoggerManager.getLogger("io.joshuasalcedo.logging").setLevel(LogLevel.WARN);
        }
    }
    
    /**
     * Configuration class for development environments
     */
    @Configuration
    @ConditionalOnProperty(
        prefix = "joshuasalcedo.logging",
        name = "profile",
        havingValue = "development",
        matchIfMissing = true
    )
    static class DevelopmentConfiguration {
        
        public DevelopmentConfiguration() {
            // Enable more verbose logging in development
            LoggerManager.getLogger("io.joshuasalcedo.logging").setLevel(LogLevel.DEBUG);
        }
    }
}