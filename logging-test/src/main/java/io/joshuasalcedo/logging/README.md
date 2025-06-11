# Async Logging Demo

This directory contains a demo application that showcases the async logging functionality of the Joshua Salcedo Logging Framework.

## Overview

The `AsyncLoggingDemo` class demonstrates various aspects of async logging:

1. **Basic Async Logging**: Shows how to set up and use async logging with default configuration
2. **Custom Configuration**: Demonstrates how to customize the async logging behavior
3. **Performance Comparison**: Compares the performance of synchronous vs. asynchronous logging
4. **Overflow Handling**: Shows how to handle queue overflow situations
5. **TestHandler Integration**: Demonstrates how to use the TestHandler with async logging for testing

## Running the Demo

To run the demo, execute the `main` method in the `AsyncLoggingDemo` class:

```bash
# From the project root directory
mvn compile exec:java -Dexec.mainClass="io.joshuasalcedo.logging.AsyncLoggingDemo"
```

## Expected Output

The demo will output various log messages to the console, along with explanatory text for each section. You'll see:

- Basic log messages at different levels
- Performance metrics comparing sync and async logging
- Demonstration of overflow handling
- Integration with TestHandler

## Key Concepts Demonstrated

### 1. Setting Up Async Logging

```java
// Enable async logging by default
LoggerManager.setUseAsyncByDefault(true);

// Or wrap a specific handler with AsyncHandler
ConsoleHandler consoleHandler = new ConsoleHandler();
AsyncHandler asyncHandler = new AsyncHandler(consoleHandler);
logger.addHandler(asyncHandler);
```

### 2. Custom Configuration

```java
AsyncConfiguration config = new AsyncConfiguration();
config.setQueueSize(5000);         // Default: 10000
config.setThreadCount(4);          // Default: 2
config.setDiscardOnOverflow(false); // Default: true
config.setShutdownTimeoutSeconds(60); // Default: 30

AsyncHandler asyncHandler = new AsyncHandler(
    consoleHandler,
    config.getQueueSize(),
    config.isDiscardOnOverflow(),
    config.getThreadCount()
);
```

### 3. Flushing and Closing

```java
// Flush to ensure all pending logs are processed
asyncHandler.flush();

// Close the handler (will process remaining logs and shut down threads)
asyncHandler.close();
```

## Benefits of Async Logging

- **Improved Application Performance**: Logging operations don't block the main thread
- **Configurable Queue Size**: Control memory usage
- **Overflow Handling Options**: Choose between blocking or discarding logs when the queue is full
- **Graceful Shutdown**: Ensures all logs are processed before shutdown