package io.joshuasalcedo.logging.database.repository;

import io.joshuasalcedo.logging.core.Log;
import io.joshuasalcedo.logging.core.LogLevel;

import io.joshuasalcedo.logging.database.config.DatabaseConfiguration;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

public class LogRepository {
    private final Connection connection;
    private final DatabaseConfiguration config;
    private final String tableName = "logs";
    private volatile boolean initialized = false;
    
    public LogRepository(DatabaseConfiguration config) throws SQLException {
        this.config = config;
        config.validate(); // Validate configuration before proceeding
        
        this.connection = createConnection();
        initializeDatabase();
    }
    
    // Legacy constructor for backward compatibility
    public LogRepository(String databaseName) throws SQLException {
        this.config = new DatabaseConfiguration();
        config.setDatabaseName(databaseName);
        config.setPersistentDatabase(true);
        config.setSchemaAction(DatabaseConfiguration.SchemaAction.UPDATE);
        
        this.connection = createConnection();
        initializeDatabase();
    }
    
    private Connection createConnection() throws SQLException {
        try {
            Class.forName(config.getDriverClassName());
        } catch (ClassNotFoundException e) {
            throw new SQLException("Database driver not found: " + config.getDriverClassName(), e);
        }
        
        String jdbcUrl = config.getJdbcUrl();
        Connection conn = DriverManager.getConnection(jdbcUrl, config.getUsername(), config.getPassword());
        conn.setAutoCommit(config.isAutoCommit());
        
        if (config.isShowSql()) {
            System.out.println("Connected to database: " + jdbcUrl);
        }
        
        return conn;
    }
    
    private synchronized void initializeDatabase() throws SQLException {
        if (initialized) {
            return;
        }
        
        try {
            switch (config.getSchemaAction()) {
                case CREATE:
                    dropTablesIfExists();
                    createTables();
                    break;
                case CREATE_DROP:
                    // Will be dropped on shutdown via connection URL or shutdown hook
                    dropTablesIfExists();
                    createTables();
                    addShutdownHook();
                    break;
                case UPDATE:
                    createTablesIfNotExists();
                    if (config.isCreateIndexesAutomatically()) {
                        createIndexes();
                    }
                    break;
                case VALIDATE:
                    validateSchema();
                    break;
                case NONE:
                    // Do nothing
                    break;
            }
            
            if (config.isEnableMigrations()) {
                runMigrations();
            }
            
            initialized = true;
            
        } catch (SQLException e) {
            throw new SQLException("Failed to initialize database schema", e);
        }
    }
    
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (config.getSchemaAction() == DatabaseConfiguration.SchemaAction.CREATE_DROP) {
                    dropTablesIfExists();
                }
                close();
            } catch (SQLException e) {
                System.err.println("Error during database shutdown: " + e.getMessage());
            }
        }));
    }
    
    private void createTablesIfNotExists() throws SQLException {
        if (!config.isCreateTablesAutomatically()) {
            return;
        }
        
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
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                
                -- Table constraints
                CONSTRAINT chk_logs_level CHECK (level IN ('DEBUG', 'INFO', 'WARN', 'ERROR', 'CRITICAL')),
                CONSTRAINT chk_logs_level_value CHECK (level_value BETWEEN 0 AND 1000),
                CONSTRAINT chk_logs_message_not_empty CHECK (LENGTH(TRIM(message)) > 0),
                CONSTRAINT chk_logs_logger_name_not_empty CHECK (LENGTH(TRIM(logger_name)) > 0)
            )
            """.formatted(tableName);
        
        try (Statement stmt = connection.createStatement()) {
            if (config.isShowSql()) {
                System.out.println("Creating table: " + (config.isFormatSql() ? formatSql(sql) : sql));
            }
            stmt.execute(sql);
        }
    }
    
    private void createTables() throws SQLException {
        dropTablesIfExists(); // Ensure clean state
        createTablesIfNotExists();
    }
    
    private void dropTablesIfExists() throws SQLException {
        String sql = "DROP TABLE IF EXISTS %s".formatted(tableName);
        
        try (Statement stmt = connection.createStatement()) {
            if (config.isShowSql()) {
                System.out.println("Dropping table: " + sql);
            }
            stmt.execute(sql);
        }
    }
    
    private void validateSchema() throws SQLException {
        // Check if required tables exist
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet tables = metaData.getTables(null, null, tableName.toUpperCase(), new String[]{"TABLE"})) {
            if (!tables.next()) {
                throw new SQLException("Required table '" + tableName + "' does not exist. Schema validation failed.");
            }
        }
        
        // Check if required columns exist
        String[] requiredColumns = {
            "id", "level", "level_value", "message", "logger_name", 
            "timestamp", "created_at"
        };
        
        try (ResultSet columns = metaData.getColumns(null, null, tableName.toUpperCase(), null)) {
            List<String> existingColumns = new ArrayList<>();
            while (columns.next()) {
                existingColumns.add(columns.getString("COLUMN_NAME").toLowerCase());
            }
            
            for (String requiredColumn : requiredColumns) {
                if (!existingColumns.contains(requiredColumn.toLowerCase())) {
                    throw new SQLException("Required column '" + requiredColumn + "' does not exist in table '" + tableName + "'. Schema validation failed.");
                }
            }
        }
        
        if (config.isShowSql()) {
            System.out.println("Schema validation passed for table: " + tableName);
        }
    }
    
    private void runMigrations() throws SQLException {
        // Simple migration system - execute SQL files from migration location
        String migrationLocation = config.getMigrationLocation();
        
        if (migrationLocation.startsWith("classpath:")) {
            String resourcePath = migrationLocation.substring(10); // Remove "classpath:"
            runClasspathMigrations(resourcePath);
        } else {
            runFileSystemMigrations(migrationLocation);
        }
    }
    
    private void runClasspathMigrations(String resourcePath) throws SQLException {
        // Try to load migration files from classpath
        String[] migrationFiles = {"V1__Create_logs_table.sql", "V2__Create_indexes.sql", "V3__Add_constraints.sql"};
        
        for (String fileName : migrationFiles) {
            String fullPath = resourcePath + "/" + fileName;
            try (InputStream is = getClass().getClassLoader().getResourceAsStream(fullPath)) {
                if (is != null) {
                    String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    executeMigrationSql(fileName, sql);
                }
            } catch (Exception e) {
                if (config.isShowSql()) {
                    System.out.println("Migration file not found or failed: " + fileName + " - " + e.getMessage());
                }
            }
        }
    }
    
    private void runFileSystemMigrations(String migrationPath) throws SQLException {
        try {
            if (Files.exists(Paths.get(migrationPath))) {
                Files.list(Paths.get(migrationPath))
                     .filter(path -> path.toString().endsWith(".sql"))
                     .sorted()
                     .forEach(path -> {
                         try {
                             String sql = Files.readString(path, StandardCharsets.UTF_8);
                             executeMigrationSql(path.getFileName().toString(), sql);
                         } catch (Exception e) {
                             throw new RuntimeException("Failed to execute migration: " + path, e);
                         }
                     });
            }
        } catch (Exception e) {
            throw new SQLException("Failed to run filesystem migrations", e);
        }
    }
    
    private void executeMigrationSql(String fileName, String sql) throws SQLException {
        if (config.isShowSql()) {
            System.out.println("Executing migration: " + fileName);
        }
        
        // Split SQL into individual statements
        String[] statements = sql.split(";");
        
        try (Statement stmt = connection.createStatement()) {
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--") && !trimmed.startsWith("#")) {
                    if (config.isShowSql() && config.isFormatSql()) {
                        System.out.println(formatSql(trimmed));
                    }
                    stmt.execute(trimmed);
                }
            }
        }
    }
    
    private String formatSql(String sql) {
        // Simple SQL formatting
        return sql.replaceAll("\s+", " ")
                 .replace(",", ",\n    ")
                 .replace(" FROM ", "\nFROM ")
                 .replace(" WHERE ", "\nWHERE ")
                 .replace(" ORDER BY ", "\nORDER BY ")
                 .replace(" GROUP BY ", "\nGROUP BY ")
                 .trim();
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
        try {
            if (connection != null && !connection.isClosed()) {
                if (config.isShowSql()) {
                    System.out.println("Closing database connection: " + config.getDatabaseName());
                }
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database connection: " + e.getMessage());
            throw e;
        }
    }
    
    // Configuration and status methods
    public DatabaseConfiguration getConfiguration() {
        return config;
    }
    
    public boolean isInitialized() {
        return initialized;
    }
    
    public boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
    
    public String getConnectionInfo() {
        try {
            if (connection != null) {
                DatabaseMetaData metaData = connection.getMetaData();
                return String.format(
                    "Database: %s %s, URL: %s, User: %s",
                    metaData.getDatabaseProductName(),
                    metaData.getDatabaseProductVersion(),
                    metaData.getURL(),
                    metaData.getUserName()
                );
            }
        } catch (SQLException e) {
            return "Connection info unavailable: " + e.getMessage();
        }
        return "No connection";
    }
    
    /**
     * Execute cleanup based on retention policy
     */
    public int performCleanup() throws SQLException {
        if (!config.isEnableAutomaticCleanup()) {
            return 0;
        }
        
        Instant cutoffTime = Instant.now().minusSeconds(config.getRetentionDays() * 24 * 60 * 60);
        
        String sql = "DELETE FROM %s WHERE created_at < ?".formatted(tableName);
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.from(cutoffTime));
            int deletedRows = stmt.executeUpdate();
            
            if (config.isShowSql() && deletedRows > 0) {
                System.out.println("Cleaned up " + deletedRows + " old log records older than " + cutoffTime);
            }
            
            return deletedRows;
        }
    }
    
    /**
     * Get database statistics
     */
    public DatabaseStatistics getStatistics() throws SQLException {
        long totalRecords = count();
        
        // Get size information
        String sizeSql = "SELECT "
            + "COUNT(*) as total_records, "
            + "MIN(created_at) as oldest_record, "
            + "MAX(created_at) as newest_record "
            + "FROM " + tableName;
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sizeSql)) {
            
            if (rs.next()) {
                return new DatabaseStatistics(
                    totalRecords,
                    rs.getTimestamp("oldest_record") != null ? rs.getTimestamp("oldest_record").toInstant() : null,
                    rs.getTimestamp("newest_record") != null ? rs.getTimestamp("newest_record").toInstant() : null,
                    isConnectionValid(),
                    getConnectionInfo()
                );
            }
        }
        
        return new DatabaseStatistics(totalRecords, null, null, isConnectionValid(), getConnectionInfo());
    }
    
    /**
     * Database statistics record
     */
    public record DatabaseStatistics(
        long totalRecords,
        Instant oldestRecord,
        Instant newestRecord,
        boolean connectionValid,
        String connectionInfo
    ) {
        @Override
        public String toString() {
            return String.format(
                "DatabaseStatistics{records=%d, oldest=%s, newest=%s, valid=%s}",
                totalRecords, oldestRecord, newestRecord, connectionValid
            );
        }
    }
}