package io.joshuasalcedo.logging.metrics.export;

/**
 * Interface for exporting metrics in various formats
 */
public interface MetricsExporter {
    
    /**
     * Export metrics as a string
     * @return formatted metrics data
     */
    String export();
    
    /**
     * Export metrics to an output stream
     * @param outputStream target stream for export
     */
    void export(java.io.OutputStream outputStream);
    
    /**
     * Get the content type for this export format
     * @return MIME type string
     */
    String getContentType();
}
