package io.joshuasalcedo.logging.async;

/**
 * Configuration for async logging
 */
public class AsyncConfiguration {
    
    private int queueSize = 10000;
    private int threadCount = 2;
    private boolean discardOnOverflow = true;
    private long shutdownTimeoutSeconds = 30;
    
    // TODO: Add getters and setters
    
    public int getQueueSize() { return queueSize; }
    public void setQueueSize(int queueSize) { this.queueSize = queueSize; }
    
    public int getThreadCount() { return threadCount; }
    public void setThreadCount(int threadCount) { this.threadCount = threadCount; }
    
    public boolean isDiscardOnOverflow() { return discardOnOverflow; }
    public void setDiscardOnOverflow(boolean discardOnOverflow) { this.discardOnOverflow = discardOnOverflow; }
    
    public long getShutdownTimeoutSeconds() { return shutdownTimeoutSeconds; }
    public void setShutdownTimeoutSeconds(long shutdownTimeoutSeconds) { this.shutdownTimeoutSeconds = shutdownTimeoutSeconds; }
}
