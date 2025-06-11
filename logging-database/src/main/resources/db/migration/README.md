# Database Migration Scripts

This directory contains SQL migration scripts for the Joshua Salcedo Logging Framework database schema.

## Migration Files

### V1__Create_logs_table.sql
- Creates the main `logs` table with all necessary columns
- Adds table and column comments for documentation
- Primary table for storing all log entries

### V2__Create_indexes.sql
- Creates performance indexes for efficient querying
- Includes single-column and composite indexes
- Optimized for common query patterns:
  - Level-based filtering
  - Time range searches
  - Logger-specific queries
  - Exception-only queries

### V3__Add_constraints.sql
- Adds data integrity constraints
- Validates log levels and values
- Ensures data consistency
- Prevents invalid data entry

### V4__Create_rollback_scripts.sql
- Creates utility views for log analysis
- Adds cleanup and archival procedures
- Provides tools for log maintenance

## Usage

### Manual Migration
Execute the scripts in order (V1, V2, V3, V4) against your database.

```sql
-- Execute in order:
\i V1__Create_logs_table.sql
\i V2__Create_indexes.sql
\i V3__Add_constraints.sql
\i V4__Create_rollback_scripts.sql
```

### Automatic Migration
The LogRepository class automatically executes V1 and V2 when creating a new database connection.

## Views Created

### v_logs_formatted
Human-readable view of logs with formatted output, useful for debugging and analysis.

```sql
SELECT * FROM v_logs_formatted WHERE level = 'ERROR' LIMIT 10;
```

### v_log_statistics
Statistical summary showing log counts by level, date ranges, and exception counts.

```sql
SELECT * FROM v_log_statistics;
```

## Utility Functions

### cleanup_old_logs(cutoff_date)
Removes logs older than the specified date.

```sql
-- Remove logs older than 30 days
SELECT cleanup_old_logs(DATEADD(DAY, -30, CURRENT_TIMESTAMP));
```

### archive_and_cleanup_logs(cutoff_date, suffix)
Archives old logs to a new table before deletion.

```sql
-- Archive logs older than 90 days
SELECT archive_and_cleanup_logs(DATEADD(DAY, -90, CURRENT_TIMESTAMP), '2024_q1');
```

## Rollback Instructions

### To rollback V4 (utilities):
```sql
DROP VIEW IF EXISTS v_logs_formatted;
DROP VIEW IF EXISTS v_log_statistics;
DROP FUNCTION IF EXISTS cleanup_old_logs;
DROP FUNCTION IF EXISTS archive_and_cleanup_logs;
```

### To rollback V3 (constraints):
```sql
ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_level;
ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_level_value;
ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_message_not_empty;
ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_logger_name_not_empty;
ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_line_number;
ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_timestamp_reasonable;
ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_created_at_after_timestamp;
ALTER TABLE logs DROP CONSTRAINT IF EXISTS chk_logs_throwable_consistency;
```

### To rollback V2 (indexes):
```sql
DROP INDEX IF EXISTS idx_logs_level;
DROP INDEX IF EXISTS idx_logs_timestamp;
DROP INDEX IF EXISTS idx_logs_logger_name;
DROP INDEX IF EXISTS idx_logs_class_name;
DROP INDEX IF EXISTS idx_logs_level_timestamp;
DROP INDEX IF EXISTS idx_logs_created_at;
DROP INDEX IF EXISTS idx_logs_has_throwable;
DROP INDEX IF EXISTS idx_logs_logger_level;
DROP INDEX IF EXISTS idx_logs_method_name;
```

### To rollback V1 (table):
```sql
DROP TABLE IF EXISTS logs;
```

## Notes

- All scripts use `IF NOT EXISTS` or `IF EXISTS` clauses to be idempotent
- Scripts are designed for H2 database but should work with most SQL databases
- Constraints ensure data integrity but may need adjustment for specific use cases
- Indexes are optimized for read-heavy workloads typical of logging systems