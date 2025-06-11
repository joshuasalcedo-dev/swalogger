package io.joshuasalcedo.logging;

import io.joshuasalcedo.logging.async.queue.MemoryLogQueue;
import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;
import io.joshuasalcedo.logging.handler.ConsoleHandler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demo application for the MemoryLogQueue functionality.
 * This class demonstrates how to use the MemoryLogQueue for asynchronous logging
 * with different configurations and scenarios.
 * 
 * <h2>Overview</h2>
 * 
 * This demo showcases four key aspects of MemoryLogQueue:
 * <ol>
 *   <li><b>Basic Usage</b>: Shows how to create and use a MemoryLogQueue</li>
 *   <li><b>Producer-Consumer Pattern</b>: Demonstrates using MemoryLogQueue in a producer-consumer scenario</li>
 *   <li><b>Capacity Management</b>: Shows how to handle queue capacity limits</li>
 *   <li><b>Performance Testing</b>: Measures throughput of the MemoryLogQueue</li>
 * </ol>
 * 
 * <h2>Running the Demo</h2>
 * 
 * To run this demo, execute the main method in this class:
 * <pre>
 * mvn compile exec:java -Dexec.mainClass="io.joshuasalcedo.logging.MemoryLogQueueDemo"
 * </pre>
 * 
 * <h2>Benefits of MemoryLogQueue</h2>
 * <ul>
 *   <li><b>Thread-Safe</b>: Built on BlockingQueue for safe concurrent access</li>
 *   <li><b>Configurable Capacity</b>: Control memory usage with capacity limits</li>
 *   <li><b>Blocking and Non-Blocking Operations</b>: Choose between offer() and put() methods</li>
 *   <li><b>Timeout Support</b>: Use poll() with timeout for controlled waiting</li>
 * </ul>
 * 
 * @see io.joshuasalcedo.logging.async.queue.MemoryLogQueue
 * @see io.joshuasalcedo.logging.async.queue.LogQueue
 */
public class MemoryLogQueueDemo {

    /**
     * Main method that runs all the MemoryLogQueue demos.
     * 
     * This method executes all four demo methods in sequence:
     * <ol>
     *   <li>Basic Usage Demo</li>
     *   <li>Producer-Consumer Demo</li>
     *   <li>Capacity Management Demo</li>
     *   <li>Performance Testing Demo</li>
     * </ol>
     * 
     * To run this demo, execute:
     * <pre>
     * mvn compile exec:java -Dexec.mainClass="io.joshuasalcedo.logging.MemoryLogQueueDemo"
     * </pre>
     * 
     * @param args command line arguments (not used)
     * @throws InterruptedException if any of the demo methods are interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== MEMORY LOG QUEUE DEMO ===");
        System.out.println("This demo shows how to use the MemoryLogQueue for asynchronous logging");
        System.out.println("with different configurations and scenarios.");
        System.out.println();

        // Demo 1: Basic Usage
        basicUsageDemo();

        // Demo 2: Producer-Consumer Pattern
        producerConsumerDemo();

        // Demo 3: Capacity Management
        capacityManagementDemo();

        // Demo 4: Performance Testing
        performanceTestingDemo();

        System.out.println("\n=== DEMO COMPLETED ===");
    }

    /**
     * Demo 1: Basic usage of MemoryLogQueue.
     * 
     * This method demonstrates the simplest way to create and use a MemoryLogQueue.
     * It shows:
     * <ul>
     *   <li>How to create a MemoryLogQueue with a specific capacity</li>
     *   <li>How to add logs to the queue using offer()</li>
     *   <li>How to retrieve logs from the queue using poll()</li>
     *   <li>How to check queue size and emptiness</li>
     * </ul>
     */
    private static void basicUsageDemo() {
        System.out.println("\n=== DEMO 1: BASIC USAGE ===");

        // Create a MemoryLogQueue with capacity of 100
        MemoryLogQueue queue = new MemoryLogQueue(100);
        System.out.println("Created MemoryLogQueue with capacity of 100");

        // Add some logs to the queue
        System.out.println("Adding logs to the queue...");
        for (int i = 0; i < 5; i++) {
            Log log = new Log(LogLevel.INFO, "Test message " + i, "MemoryLogQueueDemo");
            boolean added = queue.offer(log);
            System.out.println("Added log #" + i + ": " + added);
        }

        // Check queue size
        System.out.println("Queue size: " + queue.size());
        System.out.println("Queue is empty: " + queue.isEmpty());

        // Retrieve logs from the queue
        System.out.println("\nRetrieving logs from the queue...");
        while (!queue.isEmpty()) {
            Log log = queue.poll();
            System.out.println("Retrieved log: " + log.getMessage());
        }

        // Check queue size again
        System.out.println("Queue size after retrieval: " + queue.size());
        System.out.println("Queue is empty: " + queue.isEmpty());
    }

    /**
     * Demo 2: Using MemoryLogQueue in a producer-consumer pattern.
     * 
     * This method demonstrates how to use MemoryLogQueue in a multi-threaded
     * producer-consumer scenario. It shows:
     * <ul>
     *   <li>How to create producer threads that add logs to the queue</li>
     *   <li>How to create consumer threads that process logs from the queue</li>
     *   <li>How to use blocking operations (put/poll with timeout)</li>
     *   <li>How to coordinate shutdown between producers and consumers</li>
     * </ul>
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    private static void producerConsumerDemo() throws InterruptedException {
        System.out.println("\n=== DEMO 2: PRODUCER-CONSUMER PATTERN ===");

        // Create a MemoryLogQueue with capacity of 50
        final MemoryLogQueue queue = new MemoryLogQueue(50);
        System.out.println("Created MemoryLogQueue with capacity of 50");

        // Create a flag to signal consumers to stop
        final AtomicInteger remainingLogs = new AtomicInteger(100);
        final ConsoleHandler consoleHandler = new ConsoleHandler();

        // Create consumer thread
        Thread consumer = new Thread(() -> {
            try {
                while (remainingLogs.get() > 0 || !queue.isEmpty()) {
                    // Poll with timeout to avoid busy waiting
                    Log log = queue.poll(100, TimeUnit.MILLISECONDS);
                    if (log != null) {
                        // Process the log
                        consoleHandler.publish(log);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Create producer thread
        Thread producer = new Thread(() -> {
            try {
                for (int i = 0; i < 100; i++) {
                    Log log = new Log(LogLevel.INFO, 
                                     "Producer-Consumer log #" + i, 
                                     "MemoryLogQueueDemo");
                    // Use put() which blocks if the queue is full
                    queue.put(log);
                    remainingLogs.decrementAndGet();
                    
                    // Sleep a bit to simulate varying production rate
                    if (i % 10 == 0) {
                        Thread.sleep(50);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Start the threads
        System.out.println("Starting producer and consumer threads...");
        consumer.start();
        producer.start();

        // Wait for producer to finish
        producer.join();
        System.out.println("Producer finished");

        // Wait for consumer to finish
        consumer.join();
        System.out.println("Consumer finished");

        // Check final state
        System.out.println("Final queue size: " + queue.size());
        System.out.println("Queue is empty: " + queue.isEmpty());
    }

    /**
     * Demo 3: Managing queue capacity.
     * 
     * This method demonstrates how to handle capacity limits in MemoryLogQueue.
     * It shows:
     * <ul>
     *   <li>How to create a MemoryLogQueue with a small capacity</li>
     *   <li>How offer() behaves when the queue is full (returns false)</li>
     *   <li>How put() behaves when the queue is full (blocks)</li>
     *   <li>How to implement a strategy for handling queue overflow</li>
     * </ul>
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    private static void capacityManagementDemo() throws InterruptedException {
        System.out.println("\n=== DEMO 3: CAPACITY MANAGEMENT ===");

        // Create a MemoryLogQueue with small capacity
        final MemoryLogQueue queue = new MemoryLogQueue(5);
        System.out.println("Created MemoryLogQueue with capacity of 5");

        // Fill the queue using offer()
        System.out.println("\nFilling the queue using offer()...");
        for (int i = 0; i < 10; i++) {
            Log log = new Log(LogLevel.INFO, "Capacity test log #" + i, "MemoryLogQueueDemo");
            boolean added = queue.offer(log);
            System.out.println("Added log #" + i + ": " + added);
        }

        // Check queue size
        System.out.println("Queue size after offer() attempts: " + queue.size());

        // Clear the queue
        queue.clear();
        System.out.println("Queue cleared. Size: " + queue.size());

        // Demonstrate put() with a separate thread
        System.out.println("\nDemonstrating put() with blocking behavior...");
        Thread putThread = new Thread(() -> {
            try {
                for (int i = 0; i < 10; i++) {
                    Log log = new Log(LogLevel.INFO, 
                                     "Put test log #" + i, 
                                     "MemoryLogQueueDemo");
                    System.out.println("Attempting to put log #" + i);
                    queue.put(log);
                    System.out.println("Put log #" + i + " succeeded");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Put thread interrupted");
            }
        });

        // Start the put thread
        putThread.start();

        // Wait a bit to let the queue fill up
        Thread.sleep(500);
        
        // Check queue size
        System.out.println("Queue size while put() is running: " + queue.size());
        
        // Start a consumer to drain the queue
        System.out.println("\nStarting consumer to drain the queue...");
        Thread drainThread = new Thread(() -> {
            while (!queue.isEmpty()) {
                Log log = queue.poll();
                System.out.println("Consumed: " + log.getMessage());
                try {
                    Thread.sleep(100); // Slow consumer
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        
        drainThread.start();
        
        // Wait for threads to complete
        putThread.join();
        drainThread.join();
        
        // Check final state
        System.out.println("Final queue size: " + queue.size());
    }

    /**
     * Demo 4: Performance testing of MemoryLogQueue.
     * 
     * This method measures the throughput of MemoryLogQueue in a high-volume scenario.
     * It shows:
     * <ul>
     *   <li>How to benchmark MemoryLogQueue performance</li>
     *   <li>How to use multiple producer and consumer threads</li>
     *   <li>How to calculate throughput metrics</li>
     *   <li>How queue capacity affects performance</li>
     * </ul>
     * 
     * @throws InterruptedException if the thread is interrupted during sleep
     */
    private static void performanceTestingDemo() throws InterruptedException {
        System.out.println("\n=== DEMO 4: PERFORMANCE TESTING ===");

        // Test parameters
        final int QUEUE_CAPACITY = 10000;
        final int PRODUCER_COUNT = 4;
        final int CONSUMER_COUNT = 2;
        final int LOGS_PER_PRODUCER = 100000;
        final int TOTAL_LOGS = PRODUCER_COUNT * LOGS_PER_PRODUCER;

        // Create a MemoryLogQueue
        final MemoryLogQueue queue = new MemoryLogQueue(QUEUE_CAPACITY);
        System.out.println("Created MemoryLogQueue with capacity of " + QUEUE_CAPACITY);
        System.out.println("Using " + PRODUCER_COUNT + " producers and " + CONSUMER_COUNT + " consumers");
        System.out.println("Each producer will generate " + LOGS_PER_PRODUCER + " logs");
        System.out.println("Total logs: " + TOTAL_LOGS);

        // Counter for consumed logs
        final AtomicInteger consumedCount = new AtomicInteger(0);
        
        // Create thread pools
        ExecutorService producerPool = Executors.newFixedThreadPool(PRODUCER_COUNT);
        ExecutorService consumerPool = Executors.newFixedThreadPool(CONSUMER_COUNT);
        
        // Start timing
        long startTime = System.currentTimeMillis();
        
        // Start consumers
        for (int i = 0; i < CONSUMER_COUNT; i++) {
            consumerPool.submit(() -> {
                while (consumedCount.get() < TOTAL_LOGS) {
                    Log log = queue.poll();
                    if (log != null) {
                        consumedCount.incrementAndGet();
                    }
                }
            });
        }
        
        // Start producers
        for (int i = 0; i < PRODUCER_COUNT; i++) {
            final int producerId = i;
            producerPool.submit(() -> {
                for (int j = 0; j < LOGS_PER_PRODUCER; j++) {
                    Log log = new Log(LogLevel.INFO, 
                                     "Producer " + producerId + " log #" + j, 
                                     "MemoryLogQueueDemo");
                    try {
                        queue.put(log);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            });
        }
        
        // Shutdown producer pool and wait for completion
        producerPool.shutdown();
        producerPool.awaitTermination(1, TimeUnit.MINUTES);
        
        // Shutdown consumer pool and wait for completion
        consumerPool.shutdown();
        consumerPool.awaitTermination(1, TimeUnit.MINUTES);
        
        // Calculate metrics
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        double throughput = (double) TOTAL_LOGS / (duration / 1000.0);
        
        // Report results
        System.out.println("\nPerformance Results:");
        System.out.println("Total time: " + duration + " ms");
        System.out.println("Throughput: " + String.format("%.2f", throughput) + " logs/second");
        System.out.println("Logs produced and consumed: " + consumedCount.get());
        System.out.println("Final queue size: " + queue.size());
    }
}