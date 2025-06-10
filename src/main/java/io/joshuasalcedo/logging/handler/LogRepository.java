package io.joshuasalcedo.logging.handler;

import io.joshuasalcedo.logging.Log;
import io.joshuasalcedo.logging.LogLevel;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class LogRepository {
    private final Connection connection;
    private final String tableName = "logs";
    
    public LogRepository(String databaseName) throws SQLException {
        String url = "jdbc:h2:file:./" + databaseName + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
        this.connection = DriverManager.getConnection(url, "sa", "");
        createTableIfNotExists();
        createIndexes();
    }
    
    private void createTableIfNotExists() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS %s (
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
            )
            """.formatted(tableName);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }
    
    private void createIndexes() throws SQLException {
        String[] indexes = {
            "CREATE INDEX IF NOT EXISTS idx_logs_level ON %s (level_value)".formatted(tableName),
            "CREATE INDEX IF NOT EXISTS idx_logs_timestamp ON %s (timestamp)".formatted(tableName),
            "CREATE INDEX IF NOT EXISTS idx_logs_logger_name ON %s (logger_name)".formatted(tableName),
            "CREATE INDEX IF NOT EXISTS idx_logs_class_name ON %s (class_name)".formatted(tableName),
            "CREATE INDEX IF NOT EXISTS idx_logs_level_timestamp ON %s (level_value, timestamp)".formatted(tableName),
            "CREATE INDEX IF NOT EXISTS idx_logs_created_at ON %s (created_at)".formatted(tableName)
        };
        
        try (Statement stmt = connection.createStatement()) {
            for (String index : indexes) {
                stmt.execute(index);
            }
        }
    }
    
    public void save(Log log) throws SQLException {
        String sql = """
            INSERT INTO %s (level, level_value, message, logger_name, timestamp, 
                           class_name, method_name, line_number, throwable_message, throwable_stack_trace)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, log.getLevel().name());
            stmt.setInt(2, log.getLevel().getValue());
            stmt.setString(3, log.getMessage());
            stmt.setString(4, log.getLoggerName());
            stmt.setTimestamp(5, Timestamp.from(log.getTimestamp()));
            stmt.setString(6, log.getClassName());
            stmt.setString(7, log.getMethodName());
            stmt.setInt(8, log.getLineNumber());
            
            if (log.getThrowable() != null) {
                stmt.setString(9, log.getThrowable().getMessage());
                stmt.setString(10, getStackTraceAsString(log.getThrowable()));
            } else {
                stmt.setNull(9, Types.CLOB);
                stmt.setNull(10, Types.CLOB);
            }
            
            stmt.executeUpdate();
        }
    }
    
    public List<LogRecord> findAll() throws SQLException {
        return findAll(1000); // Default limit
    }
    
    public List<LogRecord> findAll(int limit) throws SQLException {
        String sql = "SELECT * FROM %s ORDER BY timestamp DESC LIMIT ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            return executeQuery(stmt);
        }
    }
    
    public List<LogRecord> findByLevel(LogLevel level) throws SQLException {
        return findByLevel(level, 1000);
    }
    
    public List<LogRecord> findByLevel(LogLevel level, int limit) throws SQLException {
        String sql = "SELECT * FROM %s WHERE level_value >= ? ORDER BY timestamp DESC LIMIT ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, level.getValue());
            stmt.setInt(2, limit);
            return executeQuery(stmt);
        }
    }
    
    public List<LogRecord> findByLoggerName(String loggerName) throws SQLException {
        return findByLoggerName(loggerName, 1000);
    }
    
    public List<LogRecord> findByLoggerName(String loggerName, int limit) throws SQLException {
        String sql = "SELECT * FROM %s WHERE logger_name = ? ORDER BY timestamp DESC LIMIT ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, loggerName);
            stmt.setInt(2, limit);
            return executeQuery(stmt);
        }
    }
    
    public List<LogRecord> findByClassName(String className) throws SQLException {
        return findByClassName(className, 1000);
    }
    
    public List<LogRecord> findByClassName(String className, int limit) throws SQLException {
        String sql = "SELECT * FROM %s WHERE class_name = ? ORDER BY timestamp DESC LIMIT ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, className);
            stmt.setInt(2, limit);
            return executeQuery(stmt);
        }
    }
    
    public List<LogRecord> findByTimeRange(Instant from, Instant to) throws SQLException {
        return findByTimeRange(from, to, 1000);
    }
    
    public List<LogRecord> findByTimeRange(Instant from, Instant to, int limit) throws SQLException {
        String sql = "SELECT * FROM %s WHERE timestamp BETWEEN ? AND ? ORDER BY timestamp DESC LIMIT ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(from));
            stmt.setTimestamp(2, Timestamp.from(to));
            stmt.setInt(3, limit);
            return executeQuery(stmt);
        }
    }
    
    public List<LogRecord> findByMessageContaining(String keyword) throws SQLException {
        return findByMessageContaining(keyword, 1000);
    }
    
    public List<LogRecord> findByMessageContaining(String keyword, int limit) throws SQLException {
        String sql = "SELECT * FROM %s WHERE UPPER(message) LIKE UPPER(?) ORDER BY timestamp DESC LIMIT ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + keyword + "%");
            stmt.setInt(2, limit);
            return executeQuery(stmt);
        }
    }
    
    public List<LogRecord> findWithThrowables() throws SQLException {
        return findWithThrowables(1000);
    }
    
    public List<LogRecord> findWithThrowables(int limit) throws SQLException {
        String sql = "SELECT * FROM %s WHERE throwable_message IS NOT NULL ORDER BY timestamp DESC LIMIT ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            return executeQuery(stmt);
        }
    }
    
    // Complex search with multiple filters
    public List<LogRecord> search(LogSearchCriteria criteria) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM %s WHERE 1=1".formatted(tableName));
        List<Object> params = new ArrayList<>();
        
        if (criteria.getMinLevel() != null) {
            sql.append(" AND level_value >= ?");
            params.add(criteria.getMinLevel().getValue());
        }
        
        if (criteria.getLoggerName() != null) {
            sql.append(" AND logger_name = ?");
            params.add(criteria.getLoggerName());
        }
        
        if (criteria.getClassName() != null) {
            sql.append(" AND class_name = ?");
            params.add(criteria.getClassName());
        }
        
        if (criteria.getFromTime() != null) {
            sql.append(" AND timestamp >= ?");
            params.add(Timestamp.from(criteria.getFromTime()));
        }
        
        if (criteria.getToTime() != null) {
            sql.append(" AND timestamp <= ?");
            params.add(Timestamp.from(criteria.getToTime()));
        }
        
        if (criteria.getMessageKeyword() != null) {
            sql.append(" AND UPPER(message) LIKE UPPER(?)");
            params.add("%" + criteria.getMessageKeyword() + "%");
        }
        
        if (criteria.getHasThrowables() != null) {
            if (criteria.getHasThrowables()) {
                sql.append(" AND throwable_message IS NOT NULL");
            } else {
                sql.append(" AND throwable_message IS NULL");
            }
        }
        
        sql.append(" ORDER BY timestamp DESC LIMIT ?");
        params.add(criteria.getLimit());
        
        try (PreparedStatement stmt = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            return executeQuery(stmt);
        }
    }
    
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM %s".formatted(tableName);
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getLong(1);
        }
    }
    
    public long countByLevel(LogLevel level) throws SQLException {
        String sql = "SELECT COUNT(*) FROM %s WHERE level_value >= ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, level.getValue());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }
    
    public void deleteOlderThan(Instant cutoffTime) throws SQLException {
        String sql = "DELETE FROM %s WHERE timestamp < ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(cutoffTime));
            stmt.executeUpdate();
        }
    }
    
    public void deleteAll() throws SQLException {
        String sql = "DELETE FROM %s".formatted(tableName);
        
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }
    
    private List<LogRecord> executeQuery(PreparedStatement stmt) throws SQLException {
        List<LogRecord> logRecords = new ArrayList<>();
        
        try (ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                LogRecord logRecord = LogMapper.fromResultSet(rs);
                logRecords.add(logRecord);
            }
        }
        
        return logRecords;
    }
    
    private String getStackTraceAsString(Throwable throwable) {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    public void close() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }
}