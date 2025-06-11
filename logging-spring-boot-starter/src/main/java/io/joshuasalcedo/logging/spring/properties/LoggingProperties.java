package io.joshuasalcedo.logging.spring.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration properties for Joshua Salcedo Logging Framework
 */
@ConfigurationProperties(prefix = "joshuasalcedo.logging")
public class LoggingProperties {
    
    /**
     * Enable or disable the logging framework
     */
    private boolean enabled = true;
    
    /**
     * Default log level for the root logger
     */
    private String level = "INFO";
    
    /**
     * Configuration file path (optional)
     */
    private String configFile;
    
    /**
     * Console handler configuration
     */
    private ConsoleProperties console = new ConsoleProperties();
    
    /**
     * File handler configuration
     */
    private FileProperties file = new FileProperties();
    
    /**
     * Database handler configuration
     */
    private DatabaseProperties database = new DatabaseProperties();
    
    /**
     * Async handler configuration
     */
    private AsyncProperties async = new AsyncProperties();
    
    /**
     * Metrics configuration
     */
    private MetricsProperties metrics = new MetricsProperties();
    
    /**
     * Additional logger configurations
     */
    private Map<String, String> loggers = new HashMap<>();
    
    // Console handler properties
    public static class ConsoleProperties {
        private boolean enabled = true;
        private String formatter = "pattern";
        private String pattern = "%d{yyyy-MM-dd HH:mm:ss} [%level] [%logger] - %message%n";
        private boolean useJline = false;
        private boolean useColor = true;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getFormatter() { return formatter; }
        public void setFormatter(String formatter) { this.formatter = formatter; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public boolean isUseJline() { return useJline; }
        public void setUseJline(boolean useJline) { this.useJline = useJline; }
        
        public boolean isUseColor() { return useColor; }
        public void setUseColor(boolean useColor) { this.useColor = useColor; }
    }
    
    // File handler properties
    public static class FileProperties {
        private boolean enabled = false;
        private String path = "logs/application.log";
        private String formatter = "pattern";
        private String pattern = "%d{yyyy-MM-dd HH:mm:ss} [%level] [%logger] - %message%n";
        private long maxSize = 10 * 1024 * 1024; // 10MB
        private int maxBackups = 10;
        private boolean append = true;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        
        public String getFormatter() { return formatter; }
        public void setFormatter(String formatter) { this.formatter = formatter; }
        
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        
        public long getMaxSize() { return maxSize; }
        public void setMaxSize(long maxSize) { this.maxSize = maxSize; }
        
        public int getMaxBackups() { return maxBackups; }
        public void setMaxBackups(int maxBackups) { this.maxBackups = maxBackups; }
        
        public boolean isAppend() { return append; }
        public void setAppend(boolean append) { this.append = append; }
    }
    
    // Database handler properties
    public static class DatabaseProperties {
        private boolean enabled = false;
        private String url;
        private String username;
        private String password;
        private String driverClassName;
        private int batchSize = 100;
        private long flushInterval = 5000; // 5 seconds
        private boolean createTables = true;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        
        public String getDriverClassName() { return driverClassName; }
        public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
        
        public int getBatchSize() { return batchSize; }
        public void setBatchSize(int batchSize) { this.batchSize = batchSize; }
        
        public long getFlushInterval() { return flushInterval; }
        public void setFlushInterval(long flushInterval) { this.flushInterval = flushInterval; }
        
        public boolean isCreateTables() { return createTables; }
        public void setCreateTables(boolean createTables) { this.createTables = createTables; }
    }
    
    // Async handler properties
    public static class AsyncProperties {
        private boolean enabled = false;
        private int queueSize = 10000;
        private int workerThreads = 2;
        private boolean dropOnOverflow = false;
        private long shutdownTimeout = 30000; // 30 seconds
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public int getQueueSize() { return queueSize; }
        public void setQueueSize(int queueSize) { this.queueSize = queueSize; }
        
        public int getWorkerThreads() { return workerThreads; }
        public void setWorkerThreads(int workerThreads) { this.workerThreads = workerThreads; }
        
        public boolean isDropOnOverflow() { return dropOnOverflow; }
        public void setDropOnOverflow(boolean dropOnOverflow) { this.dropOnOverflow = dropOnOverflow; }
        
        public long getShutdownTimeout() { return shutdownTimeout; }
        public void setShutdownTimeout(long shutdownTimeout) { this.shutdownTimeout = shutdownTimeout; }
    }
    
    // Metrics properties
    public static class MetricsProperties {
        private boolean enabled = true;
        private boolean autoReport = false;
        private int autoReportInterval = 5; // minutes
        private boolean export = false;
        private String exportFormat = "json";
        private String exportPath = "logs/metrics";
        private boolean actuatorIntegration = true;
        
        // Getters and setters
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        
        public boolean isAutoReport() { return autoReport; }
        public void setAutoReport(boolean autoReport) { this.autoReport = autoReport; }
        
        public int getAutoReportInterval() { return autoReportInterval; }
        public void setAutoReportInterval(int autoReportInterval) { this.autoReportInterval = autoReportInterval; }
        
        public boolean isExport() { return export; }
        public void setExport(boolean export) { this.export = export; }
        
        public String getExportFormat() { return exportFormat; }
        public void setExportFormat(String exportFormat) { this.exportFormat = exportFormat; }
        
        public String getExportPath() { return exportPath; }
        public void setExportPath(String exportPath) { this.exportPath = exportPath; }
        
        public boolean isActuatorIntegration() { return actuatorIntegration; }
        public void setActuatorIntegration(boolean actuatorIntegration) { this.actuatorIntegration = actuatorIntegration; }
    }
    
    // Main properties getters and setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getConfigFile() { return configFile; }
    public void setConfigFile(String configFile) { this.configFile = configFile; }
    
    public ConsoleProperties getConsole() { return console; }
    public void setConsole(ConsoleProperties console) { this.console = console; }
    
    public FileProperties getFile() { return file; }
    public void setFile(FileProperties file) { this.file = file; }
    
    public DatabaseProperties getDatabase() { return database; }
    public void setDatabase(DatabaseProperties database) { this.database = database; }
    
    public AsyncProperties getAsync() { return async; }
    public void setAsync(AsyncProperties async) { this.async = async; }
    
    public MetricsProperties getMetrics() { return metrics; }
    public void setMetrics(MetricsProperties metrics) { this.metrics = metrics; }
    
    public Map<String, String> getLoggers() { return loggers; }
    public void setLoggers(Map<String, String> loggers) { this.loggers = loggers; }
}