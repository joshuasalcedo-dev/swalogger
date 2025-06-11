-- Joshua Salcedo Logging Framework - Database Migration V1
-- Create main logs table

CREATE TABLE IF NOT EXISTS logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    level VARCHAR(10) NOT NULL,
    level_value INT NOT NULL,
    message CLOB NOT NULL,
    logger_name VARCHAR(255) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    class_name VARCHAR(500),
    method_name VARCHAR(255),
    line_number INT,
    throwable_message CLOB,
    throwable_stack_trace CLOB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Add comments for documentation
COMMENT ON TABLE logs IS 'Main table for storing application log entries';
COMMENT ON COLUMN logs.id IS 'Primary key, auto-incremented unique identifier';
COMMENT ON COLUMN logs.level IS 'Log level name (DEBUG, INFO, WARN, ERROR, CRITICAL)';
COMMENT ON COLUMN logs.level_value IS 'Numeric log level for filtering and comparisons';
COMMENT ON COLUMN logs.message IS 'The actual log message content';
COMMENT ON COLUMN logs.logger_name IS 'Name of the logger that created this entry';
COMMENT ON COLUMN logs.timestamp IS 'When the log entry was created';
COMMENT ON COLUMN logs.class_name IS 'Source class name where log was generated';
COMMENT ON COLUMN logs.method_name IS 'Source method name where log was generated';
COMMENT ON COLUMN logs.line_number IS 'Source line number where log was generated';
COMMENT ON COLUMN logs.throwable_message IS 'Exception message if throwable was logged';
COMMENT ON COLUMN logs.throwable_stack_trace IS 'Full stack trace if throwable was logged';
COMMENT ON COLUMN logs.created_at IS 'Database insertion timestamp';