# Memory Log Queue Demo

This directory contains a comprehensive demo application that showcases the MemoryLogQueue functionality of the Joshua Salcedo Logging Framework.

## Overview

The `MemoryLogQueueDemo` class demonstrates various aspects of using the MemoryLogQueue:

1. **Basic Usage**: Shows how to create and use a MemoryLogQueue with simple operations
2. **Producer-Consumer Pattern**: Demonstrates using MemoryLogQueue in a multi-threaded producer-consumer scenario
3. **Capacity Management**: Shows how to handle queue capacity limits and overflow situations
4. **Performance Testing**: Measures throughput of the MemoryLogQueue in a high-volume scenario

## Running the Demo

To run the demo, execute the `main` method in the `MemoryLogQueueDemo` class:

```bash
# From the project root directory
mvn compile exec:java -Dexec.mainClass="io.joshuasalcedo.logging.MemoryLogQueueDemo"
```

## What to Expect

When you run the demo, you'll see:

1. Basic operations on a MemoryLogQueue (offer, poll, size, isEmpty)
2. A producer-consumer scenario with one producer and one consumer thread
3. Demonstration of capacity management with both blocking and non-blocking operations
4. Performance metrics showing throughput in a high-volume scenario

Each section is clearly marked with headers and explanatory text.

## Key Features Demonstrated

- Creating a MemoryLogQueue with a specific capacity
- Adding logs to the queue using both offer() (non-blocking) and put() (blocking) methods
- Retrieving logs from the queue using poll() with and without timeouts
- Checking queue size and emptiness
- Handling queue overflow situations
- Coordinating multiple producer and consumer threads
- Measuring performance metrics

## Implementation Details

The MemoryLogQueue is a thread-safe implementation of the LogQueue interface that uses a LinkedBlockingQueue internally. It provides:

- Thread-safe operations for concurrent access
- Configurable capacity to control memory usage
- Both blocking and non-blocking operations
- Timeout support for controlled waiting

## Use Cases

MemoryLogQueue is particularly useful for:

1. **Asynchronous Logging**: Decoupling log generation from log processing
2. **Buffering**: Handling bursts of log events without overwhelming downstream systems
3. **Back-Pressure**: Implementing back-pressure mechanisms when log consumers can't keep up
4. **Performance Optimization**: Improving application performance by offloading logging operations

## Integration with AsyncHandler

While this demo shows the MemoryLogQueue in isolation, it's worth noting that the AsyncHandler class can be configured to use a MemoryLogQueue internally for its log processing.