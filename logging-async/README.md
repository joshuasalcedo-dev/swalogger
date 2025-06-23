# Logging Async Module

The `logging-async` module provides asynchronous logging capabilities for the Joshua Salcedo Logging Framework. It allows log events to be processed in background threads, improving application performance by reducing the impact of logging on the main application thread.

## Features

- Asynchronous log processing using a configurable thread pool
- Configurable queue size for log events
- Overflow handling (block or discard)
- Graceful shutdown with timeout
- Flush mechanism to ensure all pending logs are processed

## Usage

### Maven Dependency

```xml
<dependency>
    <groupId>io.joshuasalcedo</groupId>
    <artifactId>logging-async</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Basic Usage

```java
import io.joshuasalcedo.logging.core.Logger;
import io.joshuasalcedo.logging.manager.LoggerManager;
import io.joshuasalcedo.logging.handler.ConsoleHandler;
import io.joshuasalcedo.logging.async.AsyncHandler;

// Enable async logging by default
LoggerManager.setUseAsyncByDefault(true);

// Get a logger (will use async by default)
Logger logger = LoggerManager.getLogger("my.logger");
logger.info("This log will be processed asynchronously");

// Or wrap a specific handler with AsyncHandler
ConsoleHandler consoleHandler = new ConsoleHandler();
AsyncHandler asyncHandler = new AsyncHandler(consoleHandler);
logger.addHandler(asyncHandler);
```

### Configuration Options

The `AsyncHandler` can be configured with the following options:

```java
// Create AsyncHandler with custom configuration
AsyncConfiguration config = new AsyncConfiguration();
config.setQueueSize(5000);         // Default: 10000
config.setThreadCount(4);          // Default: 2
config.setDiscardOnOverflow(false); // Default: true
config.setShutdownTimeoutSeconds(60); // Default: 30

// Create AsyncHandler with custom queue size, overflow behavior, and thread count
AsyncHandler asyncHandler = new AsyncHandler(
    wrappedHandler,
    config.getQueueSize(),
    config.isDiscardOnOverflow(),
    config.getThreadCount()
);
```

### Flushing Logs

To ensure all pending logs are processed:

```java
// Flush all pending logs
asyncHandler.flush();
```

### Shutdown

To properly shut down the async handler:

```java
// Close the handler (will process remaining logs and shut down threads)
asyncHandler.close();
```

## Performance Considerations

- Async logging improves application throughput by offloading log processing to background threads
- Consider increasing queue size for high-volume logging
- For critical logs, consider using synchronous logging to ensure immediate processing
- Monitor dropped logs count if using discard-on-overflow mode

## Thread Safety

The async logging implementation is thread-safe and can be used from multiple application threads.