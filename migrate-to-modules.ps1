# PowerShell script to migrate logging library to modular structure
# Run this from your project root directory

Write-Host "Starting migration to modular structure..." -ForegroundColor Green

# Function to create directory if it doesn't exist
function New-DirectoryIfNotExists {
    param([string]$Path)
    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path -Force | Out-Null
        Write-Host "Created directory: $Path" -ForegroundColor Blue
    }
}

# Function to update package declaration in Java files
function Update-PackageDeclaration {
    param(
        [string]$FilePath,
        [string]$NewPackage
    )
    
    if (Test-Path $FilePath) {
        $content = Get-Content $FilePath -Raw
        # Replace package declaration
        $content = $content -replace "package io\.joshuasalcedo\.logging.*?;", "package $NewPackage;"
        
        # Update common imports
        $content = $content -replace "import io\.joshuasalcedo\.logging\.Log;", "import io.joshuasalcedo.logging.core.Log;"
        $content = $content -replace "import io\.joshuasalcedo\.logging\.LogLevel;", "import io.joshuasalcedo.logging.core.LogLevel;"
        $content = $content -replace "import io\.joshuasalcedo\.logging\.manager\.Logger;", "import io.joshuasalcedo.logging.core.Logger;"
        $content = $content -replace "import io\.joshuasalcedo\.logging\.manager\.LoggerManager;", "import io.joshuasalcedo.logging.manager.LoggerManager;"
        $content = $content -replace "import io\.joshuasalcedo\.logging\.handler\.", "import io.joshuasalcedo.logging.handler."
        
        Set-Content $FilePath $content -NoNewline
        Write-Host "Updated package in: $FilePath" -ForegroundColor Yellow
    }
}

# Create the new modular directory structure
Write-Host "Creating modular directory structure..." -ForegroundColor Cyan

# Root structure
New-DirectoryIfNotExists "logging-core"
New-DirectoryIfNotExists "logging-async"
New-DirectoryIfNotExists "logging-database" 
New-DirectoryIfNotExists "logging-metrics"
New-DirectoryIfNotExists "logging-slf4j"
New-DirectoryIfNotExists "logging-spring-boot-starter"
New-DirectoryIfNotExists "logging-test"

# Core module structure
$coreBase = "logging-core/src/main/java/io/joshuasalcedo/logging"
New-DirectoryIfNotExists "$coreBase/core"
New-DirectoryIfNotExists "$coreBase/handler"
New-DirectoryIfNotExists "$coreBase/formatter"
New-DirectoryIfNotExists "$coreBase/manager"
New-DirectoryIfNotExists "$coreBase/config"
New-DirectoryIfNotExists "$coreBase/util"
New-DirectoryIfNotExists "logging-core/src/test/java/io/joshuasalcedo/logging"

# Async module structure
$asyncBase = "logging-async/src/main/java/io/joshuasalcedo/logging/async"
New-DirectoryIfNotExists $asyncBase
New-DirectoryIfNotExists "$asyncBase/queue"
New-DirectoryIfNotExists "logging-async/src/test/java/io/joshuasalcedo/logging/async"

# Database module structure
$dbBase = "logging-database/src/main/java/io/joshuasalcedo/logging/database"
New-DirectoryIfNotExists $dbBase
New-DirectoryIfNotExists "$dbBase/repository"
New-DirectoryIfNotExists "$dbBase/config"
New-DirectoryIfNotExists "$dbBase/migration"
New-DirectoryIfNotExists "logging-database/src/test/java/io/joshuasalcedo/logging/database"

# Metrics module structure
$metricsBase = "logging-metrics/src/main/java/io/joshuasalcedo/logging/metrics"
New-DirectoryIfNotExists $metricsBase
New-DirectoryIfNotExists "$metricsBase/collector"
New-DirectoryIfNotExists "$metricsBase/export"
New-DirectoryIfNotExists "logging-metrics/src/test/java/io/joshuasalcedo/logging/metrics"

# SLF4J module structure
$slf4jBase = "logging-slf4j/src/main/java/io/joshuasalcedo/logging/slf4j"
New-DirectoryIfNotExists $slf4jBase
New-DirectoryIfNotExists "$slf4jBase/config"
New-DirectoryIfNotExists "logging-slf4j/src/test/java/io/joshuasalcedo/logging/slf4j"

# Spring Boot module structure
$springBase = "logging-spring-boot-starter/src/main/java/io/joshuasalcedo/logging/spring"
New-DirectoryIfNotExists $springBase
New-DirectoryIfNotExists "$springBase/autoconfigure"
New-DirectoryIfNotExists "$springBase/properties"
New-DirectoryIfNotExists "$springBase/actuator"

# Test module structure
$testBase = "logging-test/src/main/java/io/joshuasalcedo/logging/test"
New-DirectoryIfNotExists $testBase
New-DirectoryIfNotExists "$testBase/assertions"

Write-Host "Moving and updating existing files..." -ForegroundColor Cyan

# Move core files
if (Test-Path "src/main/java/io/joshuasalcedo/logging/Log.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/Log.java" "$coreBase/core/"
    Update-PackageDeclaration "$coreBase/core/Log.java" "io.joshuasalcedo.logging.core"
}

if (Test-Path "src/main/java/io/joshuasalcedo/logging/LogLevel.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/LogLevel.java" "$coreBase/core/"
    Update-PackageDeclaration "$coreBase/core/LogLevel.java" "io.joshuasalcedo.logging.core"
}

# Move manager files
if (Test-Path "src/main/java/io/joshuasalcedo/logging/manager/Logger.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/manager/Logger.java" "$coreBase/core/"
    Update-PackageDeclaration "$coreBase/core/Logger.java" "io.joshuasalcedo.logging.core"
}

if (Test-Path "src/main/java/io/joshuasalcedo/logging/manager/LoggerManager.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/manager/LoggerManager.java" "$coreBase/manager/"
    Update-PackageDeclaration "$coreBase/manager/LoggerManager.java" "io.joshuasalcedo.logging.manager"
}

# Move handler files to core
$handlerFiles = @("Handler.java", "ConsoleHandler.java", "FileHandler.java", "JLineHandler.java")
foreach ($file in $handlerFiles) {
    if (Test-Path "src/main/java/io/joshuasalcedo/logging/handler/$file") {
        Copy-Item "src/main/java/io/joshuasalcedo/logging/handler/$file" "$coreBase/handler/"
        Update-PackageDeclaration "$coreBase/handler/$file" "io.joshuasalcedo.logging.handler"
    }
}

# Move formatter files
if (Test-Path "src/main/java/io/joshuasalcedo/logging/handler/Formatter.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/handler/Formatter.java" "$coreBase/formatter/"
    Update-PackageDeclaration "$coreBase/formatter/Formatter.java" "io.joshuasalcedo.logging.formatter"
}

if (Test-Path "src/main/java/io/joshuasalcedo/logging/handler/LogFormatter.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/handler/LogFormatter.java" "$coreBase/formatter/SimpleFormatter.java"
    Update-PackageDeclaration "$coreBase/formatter/SimpleFormatter.java" "io.joshuasalcedo.logging.formatter"
}

$formatterFiles = @("JsonFormatter.java", "PatternFormatter.java", "StructuredFormatter.java")
foreach ($file in $formatterFiles) {
    if (Test-Path "src/main/java/io/joshuasalcedo/logging/$file") {
        Copy-Item "src/main/java/io/joshuasalcedo/logging/$file" "$coreBase/formatter/"
        Update-PackageDeclaration "$coreBase/formatter/$file" "io.joshuasalcedo.logging.formatter"
    }
}

# Move config files
if (Test-Path "src/main/java/io/joshuasalcedo/logging/config/LoggingConfiguration.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/config/LoggingConfiguration.java" "$coreBase/config/"
    Update-PackageDeclaration "$coreBase/config/LoggingConfiguration.java" "io.joshuasalcedo.logging.config"
}

# Move async files
if (Test-Path "src/main/java/io/joshuasalcedo/logging/handler/AsyncHandler.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/handler/AsyncHandler.java" "$asyncBase/"
    Update-PackageDeclaration "$asyncBase/AsyncHandler.java" "io.joshuasalcedo.logging.async"
}

# Move database files
if (Test-Path "src/main/java/io/joshuasalcedo/logging/handler/DatabaseHandler.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/handler/DatabaseHandler.java" "$dbBase/"
    Update-PackageDeclaration "$dbBase/DatabaseHandler.java" "io.joshuasalcedo.logging.database"
}

$dbRepoFiles = @("LogRepository.java", "LogMapper.java", "LogRecord.java", "LogSearchCriteria.java")
foreach ($file in $dbRepoFiles) {
    if (Test-Path "src/main/java/io/joshuasalcedo/logging/handler/$file") {
        Copy-Item "src/main/java/io/joshuasalcedo/logging/handler/$file" "$dbBase/repository/"
        Update-PackageDeclaration "$dbBase/repository/$file" "io.joshuasalcedo.logging.database.repository"
    }
}

# Move metrics files
if (Test-Path "src/main/java/io/joshuasalcedo/logging/LoggingMetrics.java") {
    Copy-Item "src/main/java/io/joshuasalcedo/logging/LoggingMetrics.java" "$metricsBase/"
    Update-PackageDeclaration "$metricsBase/LoggingMetrics.java" "io.joshuasalcedo.logging.metrics"
}

# Move SLF4J files
$slf4jFiles = @("CustomLoggerAdapter.java", "CustomLoggerFactory.java", "StaticLoggerBinder.java")
foreach ($file in $slf4jFiles) {
    if (Test-Path "src/main/java/io/joshuasalcedo/logging/config/$file") {
        Copy-Item "src/main/java/io/joshuasalcedo/logging/config/$file" "$slf4jBase/"
        Update-PackageDeclaration "$slf4jBase/$file" "io.joshuasalcedo.logging.slf4j"
    }
}

# Move test files
if (Test-Path "src/test/java/io/joshuasalcedo/logging/handler/AsyncHandlerTest.java") {
    New-DirectoryIfNotExists "logging-async/src/test/java/io/joshuasalcedo/logging/async"
    Copy-Item "src/test/java/io/joshuasalcedo/logging/handler/AsyncHandlerTest.java" "logging-async/src/test/java/io/joshuasalcedo/logging/async/"
}

if (Test-Path "src/test/java/io/joshuasalcedo/logging/handler/DatabaseHandlerTest.java") {
    New-DirectoryIfNotExists "logging-database/src/test/java/io/joshuasalcedo/logging/database"
    Copy-Item "src/test/java/io/joshuasalcedo/logging/handler/DatabaseHandlerTest.java" "logging-database/src/test/java/io/joshuasalcedo/logging/database/"
}

if (Test-Path "src/test/java/io/joshuasalcedo/logging/manager/LoggerManagerDatabaseTest.java") {
    New-DirectoryIfNotExists "logging-core/src/test/java/io/joshuasalcedo/logging/manager"
    Copy-Item "src/test/java/io/joshuasalcedo/logging/manager/LoggerManagerDatabaseTest.java" "logging-core/src/test/java/io/joshuasalcedo/logging/manager/"
}

Write-Host "Creating missing classes..." -ForegroundColor Cyan

# Create missing core classes
@"
package io.joshuasalcedo.logging.core;

import io.joshuasalcedo.logging.manager.LoggerManager;
import io.joshuasalcedo.logging.config.LoggingConfiguration;

/**
 * Main entry point for the logging library
 * Provides a simple API for common use cases
 */
public final class LoggingFacade {
    
    private LoggingFacade() {}
    
    /**
     * Get a logger for the calling class
     */
    public static Logger getLogger() {
        StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
        return LoggerManager.getLogger(caller.getClassName());
    }
    
    /**
     * Get a logger by name
     */
    public static Logger getLogger(String name) {
        return LoggerManager.getLogger(name);
    }
    
    /**
     * Get a logger for a class
     */
    public static Logger getLogger(Class<?> clazz) {
        return LoggerManager.getLogger(clazz);
    }
    
    /**
     * Configure logging with default settings
     */
    public static void configure() {
        // TODO: Apply default configuration
    }
    
    /**
     * Configure logging from file
     */
    public static void configure(String configFile) {
        // TODO: Load configuration from file
    }
}
"@ | Set-Content "$coreBase/core/LoggingFacade.java"

# Create utility classes
@"
package io.joshuasalcedo.logging.util;

/**
 * Custom exceptions for the logging framework
 */
public class LoggingException extends RuntimeException {
    
    public LoggingException(String message) {
        super(message);
    }
    
    public LoggingException(String message, Throwable cause) {
        super(message, cause);
    }
}
"@ | Set-Content "$coreBase/util/LoggingException.java"

@"
package io.joshuasalcedo.logging.util;

/**
 * Utility functions for string manipulation
 */
public final class StringUtils {
    
    private StringUtils() {}
    
    // TODO: Add utility methods for string formatting, escaping, etc.
    
    public static boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}
"@ | Set-Content "$coreBase/util/StringUtils.java"

# Create abstract handler
@"
package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.formatter.Formatter;
import io.joshuasalcedo.logging.formatter.SimpleFormatter;

/**
 * Base implementation for handlers
 */
public abstract class AbstractHandler implements Handler {
    
    protected Formatter formatter = new SimpleFormatter();
    protected LogLevel level = LogLevel.INFO;
    
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
    
    protected boolean isLoggable(Log record) {
        return record.getLevel().getValue() >= level.getValue();
    }
}
"@ | Set-Content "$coreBase/handler/AbstractHandler.java"

# Create async queue interfaces
@"
package io.joshuasalcedo.logging.async.queue;

import io.joshuasalcedo.logging.core.Log;

/**
 * Interface for log queues used in async processing
 */
public interface LogQueue {
    
    boolean offer(Log log);
    
    Log poll();
    
    Log poll(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException;
    
    void put(Log log) throws InterruptedException;
    
    int size();
    
    boolean isEmpty();
    
    void clear();
}
"@ | Set-Content "$asyncBase/queue/LogQueue.java"

@"
package io.joshuasalcedo.logging.async.queue;

import io.joshuasalcedo.logging.core.Log;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Memory-based implementation of LogQueue
 */
public class MemoryLogQueue implements LogQueue {
    
    private final BlockingQueue<Log> queue;
    
    public MemoryLogQueue(int capacity) {
        this.queue = new LinkedBlockingQueue<>(capacity);
    }
    
    @Override
    public boolean offer(Log log) {
        return queue.offer(log);
    }
    
    @Override
    public Log poll() {
        return queue.poll();
    }
    
    @Override
    public Log poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }
    
    @Override
    public void put(Log log) throws InterruptedException {
        queue.put(log);
    }
    
    @Override
    public int size() {
        return queue.size();
    }
    
    @Override
    public boolean isEmpty() {
        return queue.isEmpty();
    }
    
    @Override
    public void clear() {
        queue.clear();
    }
}
"@ | Set-Content "$asyncBase/queue/MemoryLogQueue.java"

# Create async configuration
@"
package io.joshuasalcedo.logging.async;

/**
 * Configuration for async logging
 */
public class AsyncConfiguration {
    
    private int queueSize = 10000;
    private int threadCount = 2;
    private boolean discardOnOverflow = true;
    private long shutdownTimeoutSeconds = 30;
    
    // TODO: Add getters and setters
    
    public int getQueueSize() { return queueSize; }
    public void setQueueSize(int queueSize) { this.queueSize = queueSize; }
    
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    
    public boolean isDiscardOnOverflow() { return discardOnOverflow; }
    public void setDiscardOnOverflow(boolean discardOnOverflow) { this.discardOnOverflow = discardOnOverflow; }
    
    public long getShutdownTimeoutSeconds() { return shutdownTimeoutSeconds; }
    public void setShutdownTimeoutSeconds(long shutdownTimeoutSeconds) { this.shutdownTimeoutSeconds = shutdownTimeoutSeconds; }
}
"@ | Set-Content "$asyncBase/AsyncConfiguration.java"

# Create database configuration
@"
package io.joshuasalcedo.logging.database.config;

/**
 * Configuration for database logging
 */
public class DatabaseConfiguration {
    
    private String databaseName = "logs";
    private String url;
    private String username = "sa";
    private String password = "";
    private int maxConnections = 10;
    private boolean createTablesAutomatically = true;
    
    // TODO: Add getters and setters
    
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    
    public boolean isCreateTablesAutomatically() { return createTablesAutomatically; }
    public void setCreateTablesAutomatically(boolean createTablesAutomatically) { this.createTablesAutomatically = createTablesAutomatically; }
}
"@ | Set-Content "$dbBase/config/DatabaseConfiguration.java"

# Create metrics classes
@"
package io.joshuasalcedo.logging.metrics.collector;

import io.joshuasalcedo.logging.core.LogLevel;

/**
 * Interface for collecting logging metrics
 */
public interface MetricsCollector {
    
    void recordLog(LogLevel level, String loggerName, String handlerName, long processingTimeNanos);
    
    void recordDroppedLog();
    
    void recordError(String errorType, Throwable error);
    
    // TODO: Add more metric collection methods
}
"@ | Set-Content "$metricsBase/collector/MetricsCollector.java"

@"
package io.joshuasalcedo.logging.metrics.export;

/**
 * Interface for exporting metrics
 */
public interface MetricsExporter {
    
    String export();
    
    void export(java.io.OutputStream outputStream);
    
    String getContentType();
    
    // TODO: Add more export methods
}
"@ | Set-Content "$metricsBase/export/MetricsExporter.java"

# Create test utilities
@"
package io.joshuasalcedo.logging.test;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.handler.Handler;
import io.joshuasalcedo.logging.formatter.Formatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Test handler that captures logs in memory for testing
 */
public class TestHandler implements Handler {
    
    private final List<Log> logs = new ArrayList<>();
    private Formatter formatter;
    private LogLevel level = LogLevel.DEBUG;
    
    @Override
    public void publish(Log record) {
        if (record.getLevel().getValue() >= level.getValue()) {
            logs.add(record);
        }
    }
    
    public List<Log> getLogs() {
        return new ArrayList<>(logs);
    }
    
    public void clear() {
        logs.clear();
    }
    
    public int getLogCount() {
        return logs.size();
    }
    
    public Log getLastLog() {
        return logs.isEmpty() ? null : logs.get(logs.size() - 1);
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
        logs.clear();
    }
}
"@ | Set-Content "$testBase/TestHandler.java"

Write-Host "Creating build files..." -ForegroundColor Cyan

# Create parent pom.xml
@"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>io.joshuasalcedo</groupId>
    <artifactId>logging-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>Joshua Salcedo Logging Framework</name>
    <description>A modular Java logging framework</description>

    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        
        <junit.version>5.9.2</junit.version>
        <assertj.version>3.24.2</assertj.version>
        <mockito.version>5.1.1</mockito.version>
        <slf4j.version>2.0.6</slf4j.version>
        <jackson.version>2.14.2</jackson.version>
        <h2.version>2.1.214</h2.version>
        <lombok.version>1.18.26</lombok.version>
    </properties>

    <modules>
        <module>logging-core</module>
        <module>logging-async</module>
        <module>logging-database</module>
        <module>logging-metrics</module>
        <module>logging-slf4j</module>
        <module>logging-spring-boot-starter</module>
        <module>logging-test</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <!-- Internal modules -->
            <dependency>
                <groupId>io.joshuasalcedo</groupId>
                <artifactId>logging-core</artifactId>
                <version>`${project.version}</version>
            </dependency>
            
            <!-- External dependencies -->
            <dependency>
                <groupId>org.slf4j</groupId>
                <artifactId>slf4j-api</artifactId>
                <version>`${slf4j.version}</version>
            </dependency>
            
            <dependency>
                <groupId>com.fasterxml.jackson.core</groupId>
                <artifactId>jackson-databind</artifactId>
                <version>`${jackson.version}</version>
            </dependency>
            
            <dependency>
                <groupId>com.h2database</groupId>
                <artifactId>h2</artifactId>
                <version>`${h2.version}</version>
            </dependency>
            
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>`${lombok.version}</version>
            </dependency>
            
            <!-- Test dependencies -->
            <dependency>
                <groupId>org.junit.jupiter</groupId>
                <artifactId>junit-jupiter</artifactId>
                <version>`${junit.version}</version>
                <scope>test</scope>
            </dependency>
            
            <dependency>
                <groupId>org.assertj</groupId>
                <artifactId>assertj-core</artifactId>
                <version>`${assertj.version}</version>
                <scope>test</scope>
            </dependency>
            
            <dependency>
                <groupId>org.mockito</groupId>
                <artifactId>mockito-core</artifactId>
                <version>`${mockito.version}</version>
                <scope>test</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.11.0</version>
                    <configuration>
                        <source>`${maven.compiler.source}</source>
                        <target>`${maven.compiler.target}</target>
                    </configuration>
                </plugin>
                
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <version>3.0.0-M9</version>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
"@ | Set-Content "pom-new.xml"

# Create core module pom.xml
@"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.joshuasalcedo</groupId>
        <artifactId>logging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>logging-core</artifactId>
    <name>Logging Core</name>
    <description>Core logging functionality</description>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
"@ | Set-Content "logging-core/pom.xml"

# Create async module pom.xml
@"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.joshuasalcedo</groupId>
        <artifactId>logging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>logging-async</artifactId>
    <name>Logging Async</name>
    <description>Asynchronous logging support</description>

    <dependencies>
        <dependency>
            <groupId>io.joshuasalcedo</groupId>
            <artifactId>logging-core</artifactId>
        </dependency>
        
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.awaitility</groupId>
            <artifactId>awaitility</artifactId>
            <version>4.2.0</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
"@ | Set-Content "logging-async/pom.xml"

# Create database module pom.xml
@"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.joshuasalcedo</groupId>
        <artifactId>logging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>logging-database</artifactId>
    <name>Logging Database</name>
    <description>Database logging support</description>

    <dependencies>
        <dependency>
            <groupId>io.joshuasalcedo</groupId>
            <artifactId>logging-core</artifactId>
        </dependency>
        
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
        </dependency>
        
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
        
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <version>1.17.6</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
"@ | Set-Content "logging-database/pom.xml"

# Create metrics module pom.xml
@"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.joshuasalcedo</groupId>
        <artifactId>logging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>logging-metrics</artifactId>
    <name>Logging Metrics</name>
    <description>Metrics and monitoring for logging</description>

    <dependencies>
        <dependency>
            <groupId>io.joshuasalcedo</groupId>
            <artifactId>logging-core</artifactId>
        </dependency>
        
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
"@ | Set-Content "logging-metrics/pom.xml"

# Create SLF4J module pom.xml
@"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.joshuasalcedo</groupId>
        <artifactId>logging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>logging-slf4j</artifactId>
    <name>Logging SLF4J</name>
    <description>SLF4J compatibility layer</description>

    <dependencies>
        <dependency>
            <groupId>io.joshuasalcedo</groupId>
            <artifactId>logging-core</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-api</artifactId>
        </dependency>
        
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
"@ | Set-Content "logging-slf4j/pom.xml"

# Create test module pom.xml
@"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.joshuasalcedo</groupId>
        <artifactId>logging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>logging-test</artifactId>
    <name>Logging Test Utilities</name>
    <description>Test utilities for logging framework</description>

    <dependencies>
        <dependency>
            <groupId>io.joshuasalcedo</groupId>
            <artifactId>logging-core</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.junit.jupiter</groupId>
            <artifactId>junit-jupiter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.assertj</groupId>
            <artifactId>assertj-core</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.mockito</groupId>
            <artifactId>mockito-core</artifactId>
        </dependency>
    </dependencies>
</project>
"@ | Set-Content "logging-test/pom.xml"

# Create Spring Boot starter pom.xml
@"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>io.joshuasalcedo</groupId>
        <artifactId>logging-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>logging-spring-boot-starter</artifactId>
    <name>Logging Spring Boot Starter</name>
    <description>Spring Boot auto-configuration for logging framework</description>

    <properties>
        <spring-boot.version>3.0.4</spring-boot.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.joshuasalcedo</groupId>
            <artifactId>logging-core</artifactId>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
            <version>`${spring-boot.version}</version>
        </dependency>
        
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <version>`${spring-boot.version}</version>
            <optional>true</optional>
        </dependency>
        
        <!-- Test dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>`${spring-boot.version}</version>
            <scope>test</scope>
        </dependency>
    </dependencies>
</project>
"@ | Set-Content "logging-spring-boot-starter/pom.xml"

Write-Host "Creating additional configuration files..." -ForegroundColor Cyan

# Create gitignore
@"
# Maven
target/
pom.xml.tag
pom.xml.releaseBackup
pom.xml.versionsBackup
pom.xml.next
release.properties
dependency-reduced-pom.xml
buildNumber.properties
.mvn/timing.properties

# IDE
.idea/
*.iml
.vscode/
*.swp
*.swo
*~

# OS
.DS_Store
Thumbs.db

# Logs
*.log
logs/

# Database
*.db
*.mv.db
*.trace.db
"@ | Set-Content ".gitignore"

# Create README
@"
# Joshua Salcedo Logging Framework

A modular Java logging framework with support for async processing, database persistence, and metrics collection.

## Modules

- **logging-core**: Core logging functionality
- **logging-async**: Asynchronous logging support  
- **logging-database**: Database persistence
- **logging-metrics**: Metrics and monitoring
- **logging-slf4j**: SLF4J compatibility
- **logging-spring-boot-starter**: Spring Boot integration
- **logging-test**: Test utilities

## Quick Start

```java
import io.joshuasalcedo.logging.core.LoggingFacade;
import io.joshuasalcedo.logging.core.Logger;

Logger logger = LoggingFacade.getLogger();
logger.info("Hello, World!");
```

## Building

```bash
mvn clean install
```

## Usage

Add the modules you need to your project:

```xml
<dependency>
    <groupId>io.joshuasalcedo</groupId>
    <artifactId>logging-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

For async support:
```xml
<dependency>
    <groupId>io.joshuasalcedo</groupId>
    <artifactId>logging-async</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

For database support:
```xml
<dependency>
    <groupId>io.joshuasalcedo</groupId>
    <artifactId>logging-database</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```
"@ | Set-Content "README.md"

Write-Host "Migration completed successfully!" -ForegroundColor Green
Write-Host ""
Write-Host "Next steps:" -ForegroundColor Yellow
Write-Host "1. Review the generated files and update package imports as needed" -ForegroundColor White
Write-Host "2. Replace the old pom.xml with pom-new.xml: mv pom-new.xml pom.xml" -ForegroundColor White  
Write-Host "3. Test the build: mvn clean compile" -ForegroundColor White
Write-Host "4. Run tests: mvn test" -ForegroundColor White
Write-Host "5. Remove the old src/ directory when satisfied: rm -rf src/" -ForegroundColor White
Write-Host ""
Write-Host "Your logging library is now modular and ready for distribution!" -ForegroundColor Green
