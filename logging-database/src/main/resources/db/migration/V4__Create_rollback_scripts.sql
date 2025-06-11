-- Joshua Salcedo Logging Framework - Database Migration V4
-- Rollback scripts and utility procedures

-- Create a view for easy log querying with human-readable format
CREATE OR REPLACE VIEW v_logs_formatted AS
SELECT 
    id,
    level,
    timestamp,
    logger_name,
    message,
    class_name,
    method_name,
    line_number,
    CASE 
        WHEN throwable_message IS NOT NULL THEN 'YES'
        ELSE 'NO'
    END as has_exception,
    throwable_message,
    created_at
FROM logs
ORDER BY timestamp DESC;

-- Create a view for log statistics
CREATE OR REPLACE VIEW v_log_statistics AS
SELECT 
    level,
    COUNT(*) as log_count,
    MIN(timestamp) as first_log,
    MAX(timestamp) as last_log,
    COUNT(CASE WHEN throwable_message IS NOT NULL THEN 1 END) as exception_count
FROM logs
GROUP BY level
ORDER BY 
    CASE level
        WHEN 'CRITICAL' THEN 1
        WHEN 'ERROR' THEN 2
        WHEN 'WARN' THEN 3
        WHEN 'INFO' THEN 4
        WHEN 'DEBUG' THEN 5
    END;

-- Create a procedure for log cleanup (removing old logs)
-- Note: H2 syntax for procedures
CREATE OR REPLACE FUNCTION cleanup_old_logs(cutoff_date TIMESTAMP)
RETURNS INT AS
$$
BEGIN
    DELETE FROM logs WHERE created_at < cutoff_date;
    RETURN ROW_COUNT();
END;
$$;

-- Create a procedure for archiving logs before deletion
CREATE OR REPLACE FUNCTION archive_and_cleanup_logs(cutoff_date TIMESTAMP, archive_table_suffix VARCHAR(50))
RETURNS INT AS
$$
DECLARE
    archive_table_name VARCHAR(100);
    archived_count INT;
BEGIN
    -- Create archive table name
    archive_table_name := 'logs_archive_' || archive_table_suffix;
    
    -- Create archive table with same structure as logs
    EXECUTE 'CREATE TABLE ' || archive_table_name || ' AS SELECT * FROM logs WHERE 1=0';
    
    -- Insert old logs into archive
    EXECUTE 'INSERT INTO ' || archive_table_name || ' SELECT * FROM logs WHERE created_at < ''' || cutoff_date || '''';
    GET DIAGNOSTICS archived_count = ROW_COUNT;
    
    -- Delete old logs from main table
    DELETE FROM logs WHERE created_at < cutoff_date;
    
    RETURN archived_count;
END;
$$;

-- Add some helpful comments
COMMENT ON VIEW v_logs_formatted IS 'Human-readable view of logs with formatted output';
COMMENT ON VIEW v_log_statistics IS 'Statistical summary of logs by level';
COMMENT ON FUNCTION cleanup_old_logs IS 'Removes logs older than specified date, returns count of deleted rows';
COMMENT ON FUNCTION archive_and_cleanup_logs IS 'Archives old logs to a new table before deletion';