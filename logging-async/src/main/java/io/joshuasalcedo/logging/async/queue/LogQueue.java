package io.joshuasalcedo.logging.async.queue;

import io.joshuasalcedo.logging.core.Log;

/**
 * Interface for log queues used in async processing
 */
public interface LogQueue {
    
    boolean offer(Log log);
    
    Log poll();
    
    Log poll(long timeout, java.util.concurrent.TimeUnit unit) throws InterruptedException;
    
    void put(Log log) throws InterruptedException;
    
    int size();
    
    boolean isEmpty();
    
    void clear();
}
