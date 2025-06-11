package io.joshuasalcedo.logging.async;

import io.joshuasalcedo.logging.async.AsyncHandler;
import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.core.Logger;
import io.joshuasalcedo.logging.formatter.Formatter;
import io.joshuasalcedo.logging.handler.ConsoleHandler;
import io.joshuasalcedo.logging.handler.Handler;
import io.joshuasalcedo.logging.manager.LoggerManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class AsyncHandlerTest {

    // Custom handler to track logs
    private static class TestHandler implements Handler {
        private final List<Log> logs = new ArrayList<>();
        private final CountDownLatch latch;
        private LogLevel level = LogLevel.DEBUG;

        public TestHandler(CountDownLatch latch) {
            this.latch = latch;
        }

        @Override
        public synchronized void publish(Log record) {
            if (record.getLevel().getValue() >= level.getValue()) {
                logs.add(record);
                if (latch != null) {
                    latch.countDown();
                }
            }
        }

        @Override
        public void setFormatter(Formatter formatter) {}

        @Override
        public void setLevel(LogLevel level) {
            this.level = level;
        }

        @Override
        public LogLevel getLevel() {
            return level;
        }

        @Override
        public void close() {}

        public synchronized List<Log> getLogs() {
            return new ArrayList<>(logs);
        }
    }

    @BeforeEach
    public void setUp() {
        // Ensure async is enabled
        LoggerManager.setUseAsyncByDefault(true);
    }

    @AfterEach
    public void tearDown() {
        // Clean up
        LoggerManager.reset();
    }

    @Test
    public void testAsyncLogging() throws InterruptedException {
        int logCount = 100;
        CountDownLatch latch = new CountDownLatch(logCount);
        TestHandler testHandler = new TestHandler(latch);
        AsyncHandler asyncHandler = new AsyncHandler(testHandler);

        // Log messages
        for (int i = 0; i < logCount; i++) {
            Log log = new Log(LogLevel.INFO, "Test message " + i, "TestLogger");
            asyncHandler.publish(log);
        }

        // Wait for all logs to be processed
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Not all logs were processed in time");

        // Verify all logs were received
        List<Log> receivedLogs = testHandler.getLogs();
        assertEquals(logCount, receivedLogs.size());

        // Verify order is preserved
        for (int i = 0; i < logCount; i++) {
            assertEquals("Test message " + i, receivedLogs.get(i).getMessage());
        }

        asyncHandler.close();
    }

    @Test
    public void testAsyncWithMultipleThreads() throws InterruptedException {
        int threadCount = 10;
        int logsPerThread = 100;
        int totalLogs = threadCount * logsPerThread;

        CountDownLatch latch = new CountDownLatch(totalLogs);
        TestHandler testHandler = new TestHandler(latch);
        AsyncHandler asyncHandler = new AsyncHandler(testHandler);

        AtomicInteger counter = new AtomicInteger(0);

        // Create multiple threads logging concurrently
        Thread[] threads = new Thread[threadCount];
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < logsPerThread; i++) {
                    Log log = new Log(
                        LogLevel.INFO, 
                        String.format("Thread %d - Message %d", threadId, i), 
                        "TestLogger"
                    );
                    asyncHandler.publish(log);
                    counter.incrementAndGet();
                }
            });
            threads[t].start();
        }

        // Wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }

        // Wait for all logs to be processed
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Not all logs were processed in time");

        // Verify all logs were received
        assertEquals(totalLogs, testHandler.getLogs().size());
        assertEquals(totalLogs, counter.get());

        asyncHandler.close();
    }

    @Test
    public void testQueueOverflow() throws InterruptedException {
        // Create async handler with small queue
        CountDownLatch processedLatch = new CountDownLatch(1);
        TestHandler slowHandler = new TestHandler(processedLatch) {
            @Override
            public synchronized void publish(Log record) {
                try {
                    // Simulate slow processing
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                super.publish(record);
            }
        };

        AsyncHandler asyncHandler = new AsyncHandler(slowHandler, 10, true, 1);

        // Flood with logs
        for (int i = 0; i < 100; i++) {
            Log log = new Log(LogLevel.INFO, "Message " + i, "TestLogger");
            asyncHandler.publish(log);
        }

        // Some logs should be dropped
        assertTrue(asyncHandler.getDroppedLogsCount() > 0, "Expected some logs to be dropped");

        asyncHandler.close();
    }

    @Test
    public void testFlush() throws InterruptedException {
        TestHandler testHandler = new TestHandler(null);
        AsyncHandler asyncHandler = new AsyncHandler(testHandler);

        // Log some messages
        for (int i = 0; i < 10; i++) {
            Log log = new Log(LogLevel.INFO, "Message " + i, "TestLogger");
            asyncHandler.publish(log);
        }

        // Flush to ensure all logs are processed
        asyncHandler.flush();

        // All logs should be processed
        assertEquals(10, testHandler.getLogs().size());

        asyncHandler.close();
    }

    @Test
    public void testLoggerManagerAsyncByDefault() throws Exception {
        // Reset LoggerManager
        LoggerManager.setUseAsyncByDefault(true);

        // Get a logger
        Logger logger = LoggerManager.getLogger("test.async.logger");

        // Add a test handler
        CountDownLatch latch = new CountDownLatch(5);
        TestHandler testHandler = new TestHandler(latch);
        logger.addHandler(testHandler);
        logger.setUseParentHandlers(false);

        // Log messages
        for (int i = 0; i < 5; i++) {
            logger.info("Async message " + i);
        }

        // Wait for processing
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Logs not processed in time");

        // Verify logs were received
        assertEquals(5, testHandler.getLogs().size());
    }

    @Test
    public void testShutdown() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(100);
        TestHandler testHandler = new TestHandler(latch);
        AsyncHandler asyncHandler = new AsyncHandler(testHandler);

        // Log messages
        for (int i = 0; i < 100; i++) {
            Log log = new Log(LogLevel.INFO, "Message " + i, "TestLogger");
            asyncHandler.publish(log);
        }

        // Close handler
        asyncHandler.close();

        // Verify handler is no longer running
        assertFalse(asyncHandler.isRunning());

        // Try to log after close (should be ignored)
        asyncHandler.publish(new Log(LogLevel.INFO, "After close", "TestLogger"));

        // The last message should not be in the logs
        List<Log> logs = testHandler.getLogs();
        assertTrue(logs.stream().noneMatch(log -> log.getMessage().equals("After close")));
    }

    /**
     * Main method to demonstrate async logging functionality.
     * Run this method to see logs printed to the console.
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Starting async logging demonstration...");

        // Enable async logging
        LoggerManager.setUseAsyncByDefault(true);

        // Create a logger
        Logger logger = LoggerManager.getLogger("async.demo.logger");

        // Remove default handlers and set up our own
        logger.setUseParentHandlers(false);

        // Create a console handler that will output to the console
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setLevel(LogLevel.DEBUG); // Show all logs

        // Wrap the console handler with an async handler
        AsyncHandler asyncHandler = new AsyncHandler(consoleHandler);
        logger.addHandler(asyncHandler);

        // Log some messages
        System.out.println("Sending logs to async handler...");
        for (int i = 0; i < 10; i++) {
            logger.info("Async log message #" + i);

            // Add a small delay to see the async nature
            Thread.sleep(100);
        }

        // Log different levels
        logger.debug("This is a debug message");
        logger.info("This is an info message");
        logger.warning("This is a warning message");
        logger.error("This is an error message");

        // Demonstrate async processing by logging a bunch of messages quickly
        System.out.println("Sending a burst of logs...");
        for (int i = 0; i < 100; i++) {
            logger.debug("Burst log #" + i);
        }

        // Flush to ensure all logs are processed
        System.out.println("Flushing async handler...");
        asyncHandler.flush();

        // Wait a bit to ensure all logs are displayed
        System.out.println("Waiting for all logs to be processed...");
        Thread.sleep(2000);

        // Shutdown
        System.out.println("Shutting down async handler...");
        asyncHandler.close();

        System.out.println("Async logging demonstration completed.");
    }
}
