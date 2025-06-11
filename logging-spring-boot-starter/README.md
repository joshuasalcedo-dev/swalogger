# Joshua Salcedo Logging Spring Boot Starter

Spring Boot auto-configuration for the Joshua Salcedo Logging Framework.

## Quick Start

Add the dependency to your Spring Boot project:

```xml
<dependency>
    <groupId>io.joshuasalcedo</groupId>
    <artifactId>logging-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

## Configuration

Configure the logging framework in your `application.yml` or `application.properties`:

### application.yml Example

```yaml
joshuasalcedo:
  logging:
    enabled: true
    level: INFO
    
    console:
      enabled: true
      formatter: pattern
      pattern: "%d{yyyy-MM-dd HH:mm:ss} [%level] [%logger] - %message%n"
      use-jline: false
      use-color: true
    
    file:
      enabled: true
      path: logs/application.log
      formatter: pattern
      pattern: "%d{yyyy-MM-dd HH:mm:ss} [%level] [%logger] - %message%n"
      max-size: 10485760  # 10MB
      max-backups: 10
      append: true
    
    database:
      enabled: false
      url: jdbc:h2:mem:logs
      username: sa
      password:
      driver-class-name: org.h2.Driver
      batch-size: 100
      flush-interval: 5000
      create-tables: true
    
    async:
      enabled: false
      queue-size: 10000
      worker-threads: 2
      drop-on-overflow: false
      shutdown-timeout: 30000
    
    metrics:
      enabled: true
      auto-report: false
      auto-report-interval: 5
      export: false
      export-format: json
      export-path: logs/metrics
      actuator-integration: true
    
    loggers:
      com.example.service: DEBUG
      com.example.repository: WARN
```

### application.properties Example

```properties
# Enable logging framework
joshuasalcedo.logging.enabled=true
joshuasalcedo.logging.level=INFO

# Console handler
joshuasalcedo.logging.console.enabled=true
joshuasalcedo.logging.console.formatter=pattern
joshuasalcedo.logging.console.pattern=%d{yyyy-MM-dd HH:mm:ss} [%level] [%logger] - %message%n
joshuasalcedo.logging.console.use-jline=false
joshuasalcedo.logging.console.use-color=true

# File handler
joshuasalcedo.logging.file.enabled=true
joshuasalcedo.logging.file.path=logs/application.log
joshuasalcedo.logging.file.max-size=10485760
joshuasalcedo.logging.file.max-backups=10

# Metrics
joshuasalcedo.logging.metrics.enabled=true
joshuasalcedo.logging.metrics.actuator-integration=true

# Logger levels
joshuasalcedo.logging.loggers.com.example.service=DEBUG
joshuasalcedo.logging.loggers.com.example.repository=WARN
```

## Features

### Auto-Configuration

The starter automatically configures:
- Logger manager with configured handlers
- Console handler (with optional JLine support)
- File handler with rotation
- Database handler (if enabled and dependencies available)
- Async handler wrapper
- Metrics collection and reporting

### Actuator Integration

When Spring Boot Actuator is on the classpath, the following endpoints are available:

#### Health Indicator

```
GET /actuator/health/logging
```

Provides health status of the logging system including:
- Total logs processed
- Dropped logs percentage
- Error rate
- Handler failures
- Memory usage
- Performance metrics

#### Metrics Endpoint

```
GET /actuator/loggingMetrics
```

Returns detailed metrics about the logging system.

```
GET /actuator/loggingMetrics/{format}
```

Export metrics in different formats:
- `json` - JSON format
- `csv` - CSV format
- `prometheus` - Prometheus format
- `html` - HTML dashboard

```
GET /actuator/loggingMetrics/summary
```

Get a performance summary.

```
GET /actuator/loggingMetrics/report
```

Get a detailed metrics report.

```
POST /actuator/loggingMetrics/reset
```

Reset all metrics.

## Usage in Code

```java
import io.joshuasalcedo.logging.core.Logger;
import io.joshuasalcedo.logging.manager.LoggerManager;
import org.springframework.stereotype.Service;

@Service
public class MyService {
    private static final Logger logger = LoggerManager.getInstance().getLogger(MyService.class);
    
    public void doSomething() {
        logger.info("Starting operation");
        
        try {
            // Your code here
            logger.debug("Operation details", "key", "value");
        } catch (Exception e) {
            logger.error("Operation failed", e);
        }
    }
}
```

## Formatter Types

- `simple` - Basic single-line format
- `pattern` - Customizable pattern-based format
- `json` - JSON structured format
- `structured` - Key-value structured format

## Pattern Format Placeholders

- `%d{pattern}` - Date/time with SimpleDateFormat pattern
- `%level` - Log level
- `%logger` - Logger name
- `%message` - Log message
- `%thread` - Thread name
- `%n` - New line

## Production Configuration

For production environments, consider:

```yaml
joshuasalcedo:
  logging:
    profile: production
    level: INFO
    
    console:
      enabled: false  # Disable console in production
    
    file:
      enabled: true
      path: /var/log/myapp/application.log
      max-size: 104857600  # 100MB
      max-backups: 30
    
    async:
      enabled: true  # Enable async for better performance
      queue-size: 50000
      worker-threads: 4
    
    metrics:
      export: true
      export-format: prometheus
      export-path: /var/log/myapp/metrics
```

## License

This project is licensed under the Apache License 2.0.