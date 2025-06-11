-- Joshua Salcedo Logging Framework - Database Migration V2
-- Create performance indexes for efficient log querying

-- Index for log level filtering (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_logs_level ON logs (level_value);

-- Index for timestamp-based queries (time range searches)
CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON logs (timestamp);

-- Index for logger name queries (find logs by specific logger)
CREATE INDEX IF NOT EXISTS idx_logs_logger_name ON logs (logger_name);

-- Index for class name queries (find logs by source class)
CREATE INDEX IF NOT EXISTS idx_logs_class_name ON logs (class_name);

-- Composite index for level + timestamp (most efficient for level-filtered time queries)
CREATE INDEX IF NOT EXISTS idx_logs_level_timestamp ON logs (level_value, timestamp);

-- Index for created_at column (useful for maintenance and archival)
CREATE INDEX IF NOT EXISTS idx_logs_created_at ON logs (created_at);

-- Index for finding logs with exceptions
CREATE INDEX IF NOT EXISTS idx_logs_has_throwable ON logs (throwable_message) WHERE throwable_message IS NOT NULL;

-- Composite index for logger + level (useful for per-logger analytics)
CREATE INDEX IF NOT EXISTS idx_logs_logger_level ON logs (logger_name, level_value);

-- Index for method name queries (debugging specific methods)
CREATE INDEX IF NOT EXISTS idx_logs_method_name ON logs (method_name) WHERE method_name IS NOT NULL;