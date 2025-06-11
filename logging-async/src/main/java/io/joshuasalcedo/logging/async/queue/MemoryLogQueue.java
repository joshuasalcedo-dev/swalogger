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
