-- Joshua Salcedo Logging Framework - Database Migration V3
-- Add constraints for data integrity and validation

-- Check constraint for valid log levels
ALTER TABLE logs ADD CONSTRAINT chk_logs_level 
    CHECK (level IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL'));

-- Check constraint for valid level values
ALTER TABLE logs ADD CONSTRAINT chk_logs_level_value 
    CHECK (level_value BETWEEN 0 AND 50);

-- Check constraint for non-empty message
ALTER TABLE logs ADD CONSTRAINT chk_logs_message_not_empty 
    CHECK (LENGTH(TRIM(message)) > 0);

-- Check constraint for non-empty logger name
ALTER TABLE logs ADD CONSTRAINT chk_logs_logger_name_not_empty 
    CHECK (LENGTH(TRIM(logger_name)) > 0);

-- Check constraint for valid line numbers (must be positive if provided)
ALTER TABLE logs ADD CONSTRAINT chk_logs_line_number 
    CHECK (line_number IS NULL OR line_number > 0);

-- Check constraint for timestamp not in future (with 5 minute tolerance)
ALTER TABLE logs ADD CONSTRAINT chk_logs_timestamp_reasonable 
    CHECK (timestamp <= DATEADD(MINUTE, 5, CURRENT_TIMESTAMP));

-- Check constraint for created_at not before timestamp
ALTER TABLE logs ADD CONSTRAINT chk_logs_created_at_after_timestamp 
    CHECK (created_at >= timestamp);

-- Add foreign key constraint if we had a loggers table (future enhancement)
-- This is commented out as we don't have a loggers table yet
-- ALTER TABLE logs ADD CONSTRAINT fk_logs_logger_name 
--     FOREIGN KEY (logger_name) REFERENCES loggers(name);

-- Add a constraint to ensure throwable fields are consistent
-- If throwable_message exists, throwable_stack_trace should also exist
ALTER TABLE logs ADD CONSTRAINT chk_logs_throwable_consistency 
    CHECK (
        (throwable_message IS NULL AND throwable_stack_trace IS NULL) OR
        (throwable_message IS NOT NULL AND throwable_stack_trace IS NOT NULL)
    );