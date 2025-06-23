package io.joshuasalcedo.logging;

import io.joshuasalcedo.logging.async.AsyncConfiguration;
import io.joshuasalcedo.logging.async.AsyncHandler;
import io.joshuasalcedo.logging.core.Logger;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.handler.ConsoleHandler;
import io.joshuasalcedo.logging.manager.LoggerManager;
import io.joshuasalcedo.logging.test.TestHandler;

/**
 * Demo application for the Async Logging functionality.
 * This class demonstrates how to use the async logging features
 * with different configurations and scenarios.
 * 
 * <h2>Overview</h2>
 * 
 * This demo showcases five key aspects of async logging:
 * <ol>
 *   <li><b>Basic Async Logging</b>: Shows how to set up and use async logging with default configuration</li>
 *   <li><b>Custom Configuration</b>: Demonstrates how to customize the async logging behavior</li>
 *   <li><b>Performance Comparison</b>: Compares the performance of synchronous vs. asynchronous logging</li>
 *   <li><b>Overflow Handling</b>: Shows how to handle queue overflow situations</li>
 *   <li><b>TestHandler Integration</b>: Demonstrates how to use the TestHandler with async logging for testing</li>
 * </ol>
 * 
 * <h2>Running the Demo</h2>
 * 
 * To run this demo, execute the main method in this class:
 * <pre>
 * mvn compile exec:java -Dexec.mainClass="io.joshuasalcedo.logging.AsyncLoggingDemo"
 * </pre>
 * 
 * <h2>Benefits of Async Logging</h2>
 * <ul>
 *   <li><b>Improved Application Performance</b>: Logging operations don't block the main thread</li>
 *   <li><b>Configurable Queue Size</b>: Control memory usage</li>
 *   <li><b>Overflow Handling Options</b>: Choose between blocking or discarding logs when the queue is full</li>
 *   <li><b>Graceful Shutdown</b>: Ensures all logs are processed before shutdown</li>
 * </ul>
 * 
 * @see io.joshuasalcedo.logging.async.AsyncHandler
 * @see io.joshuasalcedo.logging.async.AsyncConfiguration
 * @see io.joshuasalcedo.logging.test.TestHandler
 */
public class AsyncLoggingDemo {

    /**
     * Main method that runs all the async logging demos.
     * 
     * This method executes all five demo methods in sequence:
     * <ol>
     *   <li>Basic Async Logging Demo</li>
     *   <li>Custom Configuration Demo</li>
     *   <li>Performance Comparison Demo</li>
     *   <li>Overflow Handling Demo</li>
     *   <li>TestHandler Integration Demo</li>
     * </ol>
     * 
     * To run this demo, execute:
     * <pre>
     * mvn compile exec:java -Dexec.mainClass="io.joshuasalcedo.logging.AsyncLoggingDemo"
     * </pre>
     * 
     * @param args command line arguments (not used)
     * @throws InterruptedException if any of the demo methods are interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== ASYNC LOGGING DEMO ===");
        System.out.println("This demo shows how to use the async logging functionality");
        System.out.println("with different configurations and scenarios.");
        System.out.println();

        // Demo 1: Basic Async Logging
        basicAsyncLoggingDemo();

        // Demo 2: Custom Configuration
        customConfigurationDemo();

        // Demo 3: Performance Comparison
        performanceComparisonDemo();

        // Demo 4: Overflow Handling
        overflowHandlingDemo();

        // Demo 5: Using TestHandler with Async
        testHandlerDemo();

        System.out.println("\n=== DEMO COMPLETED ===");
    }

    /**
     * Demo 1: Basic usage of async logging.
     * 
     * This method demonstrates the simplest way to set up and use async logging.
     * It shows:
     * <ul>
     *   <li>How to enable async logging globally</li>
     *   <li>How to create and configure a ConsoleHandler</li>
     *   <li>How to wrap a handler with AsyncHandler</li>
     *   <li>How to log messages at different levels</li>
     *   <li>How to flush and close the async handler properly</li>
     * </ul>
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    private static void basicAsyncLoggingDemo() throws InterruptedException {
        System.out.println("\n=== DEMO 1: BASIC ASYNC LOGGING ===");

        // Reset LoggerManager to start fresh
        LoggerManager.reset();

        // Enable async logging by default
        LoggerManager.setUseAsyncByDefault(true);

        // Get a logger (will use async by default)
        Logger logger = LoggerManager.getLogger("demo.basic");
        logger.setUseParentHandlers(false);

        // Create a console handler that will output to the console
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(LogLevel.DEBUG); // Show all logs

        // Wrap the console handler with an async handler
        AsyncHandler asyncHandler = new AsyncHandler(consoleHandler);
        logger.addHandler(asyncHandler);

        System.out.println("Sending logs to async handler...");

        // Log some messages
        for (int i = 0; i < 5; i++) {
            logger.info("Basic async log message #" + i);
            Thread.sleep(100); // Small delay to see the async nature
        }

        // Log different levels
        logger.debug("This is a debug message");
        logger.info("This is an info message");
        logger.warning("This is a warning message");
        logger.error("This is an error message");

        // Flush to ensure all logs are processed
        asyncHandler.flush();

        // Wait a bit to ensure all logs are displayed
        Thread.sleep(500);

        // Shutdown
        asyncHandler.close();
    }

    /**
     * Demo 2: Using custom configuration for async logging.
     * 
     * This method demonstrates how to customize the AsyncHandler configuration.
     * It shows:
     * <ul>
     *   <li>How to create and configure an AsyncConfiguration object</li>
     *   <li>How to set custom queue size, thread count, and overflow behavior</li>
     *   <li>How to create an AsyncHandler with custom configuration</li>
     *   <li>How to apply the custom configuration to logging</li>
     * </ul>
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    private static void customConfigurationDemo() throws InterruptedException {
        System.out.println("\n=== DEMO 2: CUSTOM CONFIGURATION ===");

        // Create AsyncHandler with custom configuration
        AsyncConfiguration config = new AsyncConfiguration();
        config.setQueueSize(5000);         // Default: 10000
        config.setThreadCount(4);          // Default: 2
        config.setDiscardOnOverflow(false); // Default: true
        config.setShutdownTimeoutSeconds(60); // Default: 30

        System.out.println("Custom configuration:");
        System.out.println("- Queue Size: " + config.getQueueSize());
        System.out.println("- Thread Count: " + config.getThreadCount());
        System.out.println("- Discard On Overflow: " + config.isDiscardOnOverflow());
        System.out.println("- Shutdown Timeout: " + config.getShutdownTimeoutSeconds() + " seconds");

        // Create a logger
        Logger logger = LoggerManager.getLogger("demo.custom");
        logger.setUseParentHandlers(false);

        // Create a console handler
        ConsoleHandler consoleHandler = new ConsoleHandler();

        // Create AsyncHandler with custom configuration
        AsyncHandler asyncHandler = new AsyncHandler(
            consoleHandler,
            config.getQueueSize(),
            config.isDiscardOnOverflow(),
            config.getThreadCount()
        );

        logger.addHandler(asyncHandler);

        // Log some messages
        System.out.println("Logging with custom configuration...");
        for (int i = 0; i < 5; i++) {
            logger.info("Custom config log #" + i);
            Thread.sleep(50);
        }

        // Flush and close
        asyncHandler.flush();
        Thread.sleep(500);
        asyncHandler.close();
    }

    /**
     * Demo 3: Performance comparison between sync and async logging.
     * 
     * This method demonstrates the performance benefits of async logging.
     * It shows:
     * <ul>
     *   <li>How to set up both synchronous and asynchronous loggers</li>
     *   <li>How to measure and compare the performance of both approaches</li>
     *   <li>The significant performance improvement that async logging provides</li>
     *   <li>How to calculate and display performance metrics</li>
     * </ul>
     * 
     * The demo logs a large number of messages using both synchronous and
     * asynchronous approaches, then compares the time taken by each.
     */
    private static void performanceComparisonDemo() {
        System.out.println("\n=== DEMO 3: PERFORMANCE COMPARISON ===");

        // Create loggers
        Logger syncLogger = LoggerManager.getLogger("demo.sync");
        syncLogger.setUseParentHandlers(false);

        Logger asyncLogger = LoggerManager.getLogger("demo.async");
        asyncLogger.setUseParentHandlers(false);

        // Add handlers
        ConsoleHandler syncHandler = new ConsoleHandler();
        syncLogger.addHandler(syncHandler);

        AsyncHandler asyncHandler = new AsyncHandler(new ConsoleHandler());
        asyncLogger.addHandler(asyncHandler);

        // Performance test
        int logCount = 10000;

        System.out.println("Logging " + logCount + " messages synchronously...");
        long syncStart = System.currentTimeMillis();
        for (int i = 0; i < logCount; i++) {
            syncLogger.debug("Sync log message #" + i);
        }
        long syncEnd = System.currentTimeMillis();
        long syncTime = syncEnd - syncStart;

        System.out.println("Logging " + logCount + " messages asynchronously...");
        long asyncStart = System.currentTimeMillis();
        for (int i = 0; i < logCount; i++) {
            asyncLogger.debug("Async log message #" + i);
        }
        long asyncEnd = System.currentTimeMillis();
        long asyncTime = asyncEnd - asyncStart;

        System.out.println("Results:");
        System.out.println("- Synchronous logging time: " + syncTime + " ms");
        System.out.println("- Asynchronous logging time: " + asyncTime + " ms");
        System.out.println("- Performance improvement: " + (syncTime - asyncTime) + " ms (" + 
                          (syncTime > 0 ? (100 * (syncTime - asyncTime) / syncTime) : 0) + "%)");

        // Flush and close
        asyncHandler.flush();
        asyncHandler.close();
    }

    /**
     * Demo 4: Handling overflow in async logging.
     * 
     * This method demonstrates how to handle queue overflow situations in async logging.
     * It shows:
     * <ul>
     *   <li>How to create a handler with slow processing (simulating a real-world bottleneck)</li>
     *   <li>How to configure AsyncHandler with a small queue to demonstrate overflow</li>
     *   <li>How to handle overflow using two different strategies:</li>
     *   <ul>
     *     <li>Discard strategy: Dropping logs when the queue is full</li>
     *     <li>Blocking strategy: Waiting for space in the queue</li>
     *   </ul>
     *   <li>How to monitor dropped logs count</li>
     * </ul>
     * 
     * This demo is particularly useful for understanding how to handle high-volume
     * logging scenarios and how to choose the appropriate overflow strategy based
     * on your application's requirements.
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    private static void overflowHandlingDemo() throws InterruptedException {
        System.out.println("\n=== DEMO 4: OVERFLOW HANDLING ===");

        // Create a logger
        Logger logger = LoggerManager.getLogger("demo.overflow");
        logger.setUseParentHandlers(false);

        // Create a console handler with a slow processing simulation
        ConsoleHandler slowHandler = new ConsoleHandler() {
            @Override
            public void publish(io.joshuasalcedo.logging.core.Log record) {
                try {
                    // Simulate slow processing
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.publish(record);
            }
        };

        // Create AsyncHandler with small queue and discard on overflow
        AsyncHandler discardHandler = new AsyncHandler(slowHandler, 10, true, 1);
        logger.addHandler(discardHandler);

        System.out.println("Logging with small queue and discard on overflow...");
        System.out.println("Sending 100 logs to a queue of size 10 with slow processing...");

        // Flood with logs
        for (int i = 0; i < 100; i++) {
            logger.info("Overflow test message #" + i);
        }

        // Wait for processing
        Thread.sleep(1000);

        // Check dropped logs
        System.out.println("Logs dropped: " + discardHandler.getDroppedLogsCount());

        // Flush and close
        discardHandler.flush();
        discardHandler.close();

        // Now try with blocking behavior
        System.out.println("\nNow trying with blocking behavior (no discard)...");

        // Create AsyncHandler with small queue and blocking behavior
        AsyncHandler blockingHandler = new AsyncHandler(slowHandler, 10, false, 1);
        logger.removeHandler(discardHandler);
        logger.addHandler(blockingHandler);

        System.out.println("Sending 20 logs to a queue of size 10 with slow processing...");
        System.out.println("This will block when the queue is full...");

        // Log some messages (fewer this time)
        for (int i = 0; i < 20; i++) {
            logger.info("Blocking test message #" + i);
        }

        // Wait for processing
        Thread.sleep(1000);

        // Flush and close
        blockingHandler.flush();
        blockingHandler.close();
    }

    /**
     * Demo 5: Using TestHandler with async logging for testing.
     * 
     * This method demonstrates how to use the TestHandler with async logging for testing purposes.
     * It shows:
     * <ul>
     *   <li>How to create and configure a TestHandler</li>
     *   <li>How to wrap a TestHandler with AsyncHandler</li>
     *   <li>How to send logs to the async TestHandler</li>
     *   <li>How to flush the async handler to ensure all logs are processed</li>
     *   <li>How to retrieve and verify captured logs from the TestHandler</li>
     * </ul>
     * 
     * This demo is particularly useful for understanding how to test asynchronous logging
     * in your applications. The TestHandler captures logs in memory, making it easy to
     * verify that the expected logs were generated without affecting external systems.
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    private static void testHandlerDemo() throws InterruptedException {
        System.out.println("\n=== DEMO 5: USING TESTHANDLER WITH ASYNC ===");

        // Create a logger
        Logger logger = LoggerManager.getLogger("demo.test");
        logger.setUseParentHandlers(false);

        // Create a TestHandler
        TestHandler testHandler = new TestHandler();

        // Wrap with AsyncHandler
        AsyncHandler asyncHandler = new AsyncHandler(testHandler);
        logger.addHandler(asyncHandler);

        System.out.println("Sending logs to async TestHandler...");

        // Log some messages
        for (int i = 0; i < 5; i++) {
            logger.info("Test handler message #" + i);
        }

        // Flush to ensure all logs are processed
        asyncHandler.flush();

        // Check captured logs
        System.out.println("Logs captured by TestHandler: " + testHandler.getLogCount());
        System.out.println("Last log message: " + 
                          (testHandler.getLastLog() != null ? testHandler.getLastLog().getMessage() : "none"));

        // Close handler
        asyncHandler.close();
    }
}
