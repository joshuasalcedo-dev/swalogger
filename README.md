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
