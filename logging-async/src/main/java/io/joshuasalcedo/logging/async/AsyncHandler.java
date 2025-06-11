package io.joshuasalcedo.logging.async;
import io.joshuasalcedo.logging.formatter.Formatter;

import io.joshuasalcedo.logging.handler.Handler;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class AsyncHandler implements Handler {
    private final Handler wrappedHandler;
    private final BlockingQueue<Log> queue;
    private final ExecutorService executor;
    private final AtomicBoolean isRunning;
    private final int maxQueueSize;
    private final boolean discardOnOverflow;
    private final AtomicInteger droppedLogs;
    
    // Default configuration
    private static final int DEFAULT_QUEUE_SIZE = 10000;
    private static final boolean DEFAULT_DISCARD_ON_OVERFLOW = true;
    private static final int DEFAULT_THREAD_COUNT = 2;
    
    public AsyncHandler(Handler handler) {
        this(handler, DEFAULT_QUEUE_SIZE, DEFAULT_DISCARD_ON_OVERFLOW, DEFAULT_THREAD_COUNT);
    }
    
    public AsyncHandler(Handler handler, int queueSize) {
        this(handler, queueSize, DEFAULT_DISCARD_ON_OVERFLOW, DEFAULT_THREAD_COUNT);
    }
    
    public AsyncHandler(Handler handler, int queueSize, boolean discardOnOverflow, int threadCount) {
        this.wrappedHandler = handler;
        this.maxQueueSize = queueSize;
        this.discardOnOverflow = discardOnOverflow;
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.isRunning = new AtomicBoolean(true);
        this.droppedLogs = new AtomicInteger(0);
        
        // Create thread pool with custom thread factory
        ThreadFactory threadFactory = new ThreadFactory() {
            private final AtomicInteger threadNumber = new AtomicInteger(1);
            
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "AsyncLogger-" + threadNumber.getAndIncrement());
                t.setDaemon(true);
                return t;
            }
        };
        
        this.executor = Executors.newFixedThreadPool(threadCount, threadFactory);
        
        // Start worker threads
        for (int i = 0; i < threadCount; i++) {
            executor.submit(this::processLogs);
        }
    }
    
    @Override
    public void publish(Log record) {
        if (!isRunning.get()) {
            return;
        }
        
        try {
            if (!queue.offer(record)) {
                // Queue is full
                if (!discardOnOverflow) {
                    // Block until space is available
                    queue.put(record);
                } else {
                    // Discard the log and increment counter
                    droppedLogs.incrementAndGet();
                    
                    // Periodically log a warning about dropped logs
                    if (droppedLogs.get() % 1000 == 0) {
                        Log warning = new Log(
                            LogLevel.WARN,
                            String.format("AsyncHandler has dropped %d logs due to queue overflow", droppedLogs.get()),
                            "AsyncHandler"
                        );
                        wrappedHandler.publish(warning);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void processLogs() {
        while (isRunning.get() || !queue.isEmpty()) {
            try {
                Log record = queue.poll(100, TimeUnit.MILLISECONDS);
                if (record != null) {
                    if (record instanceof FlushMarker) {
                        // Signal the flush completion without forwarding to the wrapped handler
                        ((FlushMarker) record).signal();
                    } else {
                        wrappedHandler.publish(record);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // Log the error but continue processing
                Log error = new Log(
                    LogLevel.ERROR,
                    "Error in async log processing: " + e.getMessage(),
                    "AsyncHandler"
                );
                try {
                    wrappedHandler.publish(error);
                } catch (Exception ignored) {
                    // If we can't even log the error, give up
                }
            }
        }
    }
    
    @Override
    public void setFormatter(Formatter formatter) {
        wrappedHandler.setFormatter(formatter);
    }
    
    @Override
    public void setLevel(LogLevel level) {
        wrappedHandler.setLevel(level);
    }
    
    @Override
    public LogLevel getLevel() {
        return wrappedHandler.getLevel();
    }
    
    @Override
    public void close() {
        // Stop accepting new logs
        isRunning.set(false);
        
        // Shutdown executor
        executor.shutdown();
        
        try {
            // Wait for pending logs to be processed
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                
                // Process remaining logs synchronously
                Log record;
                while ((record = queue.poll()) != null) {
                    try {
                        wrappedHandler.publish(record);
                    } catch (Exception ignored) {
                        // Best effort
                    }
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        
        // Close the wrapped handler
        wrappedHandler.close();
        
        // Log final statistics if any logs were dropped
        if (droppedLogs.get() > 0) {
            System.err.printf("AsyncHandler: Total logs dropped: %d%n", droppedLogs.get());
        }
    }
    
    // Delegate methods to wrapped handler
    public Handler getWrappedHandler() {
        return wrappedHandler;
    }
    
    public int getQueueSize() {
        return queue.size();
    }
    
    public int getDroppedLogsCount() {
        return droppedLogs.get();
    }
    
    public boolean isRunning() {
        return isRunning.get();
    }
    
    // Flush method to ensure all pending logs are processed
    public void flush() {
        // Use a special marker that won't be sent to the wrapped handler
        CountDownLatch latch = new CountDownLatch(1);
        
        try {
            queue.put(new FlushMarker(latch));
            // Wait for the marker to be processed
            latch.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    // Special marker class for flush operations
    private static class FlushMarker extends Log {
        private final CountDownLatch latch;
        
        public FlushMarker(CountDownLatch latch) {
            super(LogLevel.DEBUG, "FLUSH_MARKER", "AsyncHandler");
            this.latch = latch;
        }
        
        public void signal() {
            latch.countDown();
        }
    }
}