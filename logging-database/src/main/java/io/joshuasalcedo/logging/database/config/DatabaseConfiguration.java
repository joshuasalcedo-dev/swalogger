package io.joshuasalcedo.logging.database.config;

import java.util.Properties;
import java.io.InputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Comprehensive configuration for database logging with standard schema management options
 */
public class DatabaseConfiguration {
    
    // Database connection settings
    private String databaseName = "logging-db";
    private String url;
    private String username = "sa";
    private String password = "";
    private String driverClassName = "org.h2.Driver";
    private int maxConnections = 10;
    private int connectionTimeout = 30000; // 30 seconds
    private boolean autoCommit = true;
    
    // Schema management settings (following JPA/Hibernate standards)
    private SchemaAction schemaAction = SchemaAction.UPDATE;
    private boolean validateSchema = true;
    private boolean showSql = false;
    private boolean formatSql = false;
    
    // Database lifecycle settings
    private boolean createTablesAutomatically = true;
    private boolean createIndexesAutomatically = true;
    private boolean enableMigrations = true;
    private String migrationLocation = "classpath:db/migration";
    
    // Connection pool settings
    private boolean useConnectionPool = true;
    private int minPoolSize = 2;
    private int maxPoolSize = 10;
    private long maxLifetime = 1800000; // 30 minutes
    private long idleTimeout = 600000; // 10 minutes
    
    // Persistence settings
    private boolean persistentDatabase = true;
    private String databaseDirectory = "./data";
    private boolean enableCompression = false;
    private boolean enableEncryption = false;
    private String encryptionKey;
    
    // Cleanup settings
    private boolean enableAutomaticCleanup = false;
    private int retentionDays = 30;
    private String cleanupSchedule = "0 2 * * *"; // Daily at 2 AM
    
    /**
     * Schema management actions (following JPA standard)
     */
    public enum SchemaAction {
        NONE("none"),           // Do nothing
        CREATE("create"),       // Create schema, dropping existing data
        CREATE_DROP("create-drop"), // Create schema, drop on shutdown
        UPDATE("update"),       // Update schema (default)
        VALIDATE("validate");   // Validate schema only
        
        private final String value;
        
        SchemaAction(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static SchemaAction fromString(String value) {
            for (SchemaAction action : values()) {
                if (action.value.equalsIgnoreCase(value)) {
                    return action;
                }
            }
            return UPDATE; // Default
        }
    }
    
    public DatabaseConfiguration() {
        // Default configuration
    }
    
    public DatabaseConfiguration(String configFile) throws IOException {
        loadFromFile(configFile);
    }
    
    public DatabaseConfiguration(Properties properties) {
        loadFromProperties(properties);
    }
    
    private void loadFromFile(String configFile) throws IOException {
        Properties props = new Properties();
        
        // Try to load from classpath first
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(configFile)) {
            if (is != null) {
                props.load(is);
                loadFromProperties(props);
                return;
            }
        }
        
        // Try to load from filesystem
        try (InputStream is = new java.io.FileInputStream(configFile)) {
            props.load(is);
            loadFromProperties(props);
        }
    }
    
    private void loadFromProperties(Properties props) {
        // Connection settings
        this.databaseName = getStringProperty(props, "database.name", databaseName);
        this.url = getStringProperty(props, "database.url", url);
        this.username = getStringProperty(props, "database.username", username);
        this.password = getStringProperty(props, "database.password", password);
        this.driverClassName = getStringProperty(props, "database.driver", driverClassName);
        this.maxConnections = getIntProperty(props, "database.maxConnections", maxConnections);
        this.connectionTimeout = getIntProperty(props, "database.connectionTimeout", connectionTimeout);
        this.autoCommit = getBooleanProperty(props, "database.autoCommit", autoCommit);
        
        // Schema management
        String schemaActionStr = getStringProperty(props, "database.schema.action", schemaAction.getValue());
        this.schemaAction = SchemaAction.fromString(schemaActionStr);
        this.validateSchema = getBooleanProperty(props, "database.schema.validate", validateSchema);
        this.showSql = getBooleanProperty(props, "database.showSql", showSql);
        this.formatSql = getBooleanProperty(props, "database.formatSql", formatSql);
        
        // Lifecycle settings
        this.createTablesAutomatically = getBooleanProperty(props, "database.createTables", createTablesAutomatically);
        this.createIndexesAutomatically = getBooleanProperty(props, "database.createIndexes", createIndexesAutomatically);
        this.enableMigrations = getBooleanProperty(props, "database.enableMigrations", enableMigrations);
        this.migrationLocation = getStringProperty(props, "database.migrationLocation", migrationLocation);
        
        // Connection pool
        this.useConnectionPool = getBooleanProperty(props, "database.pool.enabled", useConnectionPool);
        this.minPoolSize = getIntProperty(props, "database.pool.minSize", minPoolSize);
        this.maxPoolSize = getIntProperty(props, "database.pool.maxSize", maxPoolSize);
        this.maxLifetime = getLongProperty(props, "database.pool.maxLifetime", maxLifetime);
        this.idleTimeout = getLongProperty(props, "database.pool.idleTimeout", idleTimeout);
        
        // Persistence
        this.persistentDatabase = getBooleanProperty(props, "database.persistent", persistentDatabase);
        this.databaseDirectory = getStringProperty(props, "database.directory", databaseDirectory);
        this.enableCompression = getBooleanProperty(props, "database.compression", enableCompression);
        this.enableEncryption = getBooleanProperty(props, "database.encryption", enableEncryption);
        this.encryptionKey = getStringProperty(props, "database.encryptionKey", encryptionKey);
        
        // Cleanup
        this.enableAutomaticCleanup = getBooleanProperty(props, "database.cleanup.enabled", enableAutomaticCleanup);
        this.retentionDays = getIntProperty(props, "database.cleanup.retentionDays", retentionDays);
        this.cleanupSchedule = getStringProperty(props, "database.cleanup.schedule", cleanupSchedule);
    }
    
    private boolean getBooleanProperty(Properties props, String key, boolean defaultValue) {
        String value = props.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }
    
    private int getIntProperty(Properties props, String key, int defaultValue) {
        String value = props.getProperty(key);
        try {
            return value != null ? Integer.parseInt(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private long getLongProperty(Properties props, String key, long defaultValue) {
        String value = props.getProperty(key);
        try {
            return value != null ? Long.parseLong(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
    
    private String getStringProperty(Properties props, String key, String defaultValue) {
        return props.getProperty(key, defaultValue);
    }
    
    /**
     * Get the complete JDBC URL for the database connection
     */
    public String getJdbcUrl() {
        if (url != null && !url.isEmpty()) {
            return url;
        }
        
        // Build H2 URL based on configuration
        StringBuilder urlBuilder = new StringBuilder();
        
        if (persistentDatabase) {
            // File-based database
            Path dbPath = Paths.get(databaseDirectory, databaseName);
            urlBuilder.append("jdbc:h2:file:").append(dbPath.toString());
        } else {
            // In-memory database
            urlBuilder.append("jdbc:h2:mem:").append(databaseName);
        }
        
        // Add connection parameters
        if (persistentDatabase) {
            urlBuilder.append(";AUTO_SERVER=TRUE");
        }
        urlBuilder.append(";DB_CLOSE_DELAY=-1");
        
        if (enableCompression) {
            urlBuilder.append(";COMPRESS=TRUE");
        }
        
        if (enableEncryption && encryptionKey != null && !encryptionKey.isEmpty()) {
            urlBuilder.append(";CIPHER=AES");
        }
        
        return urlBuilder.toString();
    }
    
    /**
     * Validate the configuration
     */
    public void validate() throws IllegalArgumentException {
        if (databaseName == null || databaseName.trim().isEmpty()) {
            throw new IllegalArgumentException("Database name cannot be null or empty");
        }
        
        if (username == null) {
            throw new IllegalArgumentException("Username cannot be null");
        }
        
        if (maxConnections <= 0) {
            throw new IllegalArgumentException("Max connections must be positive");
        }
        
        if (connectionTimeout <= 0) {
            throw new IllegalArgumentException("Connection timeout must be positive");
        }
        
        if (useConnectionPool) {
            if (minPoolSize < 0) {
                throw new IllegalArgumentException("Min pool size cannot be negative");
            }
            if (maxPoolSize < minPoolSize) {
                throw new IllegalArgumentException("Max pool size must be >= min pool size");
            }
        }
        
        if (retentionDays < 0) {
            throw new IllegalArgumentException("Retention days cannot be negative");
        }
        
        if (enableEncryption && (encryptionKey == null || encryptionKey.trim().isEmpty())) {
            throw new IllegalArgumentException("Encryption key required when encryption is enabled");
        }
    }
    
    // Getters and setters
    public String getDatabaseName() { return databaseName; }
    public void setDatabaseName(String databaseName) { this.databaseName = databaseName; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getDriverClassName() { return driverClassName; }
    public void setDriverClassName(String driverClassName) { this.driverClassName = driverClassName; }
    
    public int getMaxConnections() { return maxConnections; }
    public void setMaxConnections(int maxConnections) { this.maxConnections = maxConnections; }
    
    public int getConnectionTimeout() { return connectionTimeout; }
    public void setConnectionTimeout(int connectionTimeout) { this.connectionTimeout = connectionTimeout; }
    
    public boolean isAutoCommit() { return autoCommit; }
    public void setAutoCommit(boolean autoCommit) { this.autoCommit = autoCommit; }
    
    public SchemaAction getSchemaAction() { return schemaAction; }
    public void setSchemaAction(SchemaAction schemaAction) { this.schemaAction = schemaAction; }
    
    public boolean isValidateSchema() { return validateSchema; }
    public void setValidateSchema(boolean validateSchema) { this.validateSchema = validateSchema; }
    
    public boolean isShowSql() { return showSql; }
    public void setShowSql(boolean showSql) { this.showSql = showSql; }
    
    public boolean isFormatSql() { return formatSql; }
    public void setFormatSql(boolean formatSql) { this.formatSql = formatSql; }
    
    public boolean isCreateTablesAutomatically() { return createTablesAutomatically; }
    public void setCreateTablesAutomatically(boolean createTablesAutomatically) { this.createTablesAutomatically = createTablesAutomatically; }
    
    public boolean isCreateIndexesAutomatically() { return createIndexesAutomatically; }
    public void setCreateIndexesAutomatically(boolean createIndexesAutomatically) { this.createIndexesAutomatically = createIndexesAutomatically; }
    
    public boolean isEnableMigrations() { return enableMigrations; }
    public void setEnableMigrations(boolean enableMigrations) { this.enableMigrations = enableMigrations; }
    
    public String getMigrationLocation() { return migrationLocation; }
    public void setMigrationLocation(String migrationLocation) { this.migrationLocation = migrationLocation; }
    
    public boolean isUseConnectionPool() { return useConnectionPool; }
    public void setUseConnectionPool(boolean useConnectionPool) { this.useConnectionPool = useConnectionPool; }
    
    public int getMinPoolSize() { return minPoolSize; }
    public void setMinPoolSize(int minPoolSize) { this.minPoolSize = minPoolSize; }
    
    public int getMaxPoolSize() { return maxPoolSize; }
    public void setMaxPoolSize(int maxPoolSize) { this.maxPoolSize = maxPoolSize; }
    
    public long getMaxLifetime() { return maxLifetime; }
    public void setMaxLifetime(long maxLifetime) { this.maxLifetime = maxLifetime; }
    
    public long getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(long idleTimeout) { this.idleTimeout = idleTimeout; }
    
    public boolean isPersistentDatabase() { return persistentDatabase; }
    public void setPersistentDatabase(boolean persistentDatabase) { this.persistentDatabase = persistentDatabase; }
    
    public String getDatabaseDirectory() { return databaseDirectory; }
    public void setDatabaseDirectory(String databaseDirectory) { this.databaseDirectory = databaseDirectory; }
    
    public boolean isEnableCompression() { return enableCompression; }
    public void setEnableCompression(boolean enableCompression) { this.enableCompression = enableCompression; }
    
    public boolean isEnableEncryption() { return enableEncryption; }
    public void setEnableEncryption(boolean enableEncryption) { this.enableEncryption = enableEncryption; }
    
    public String getEncryptionKey() { return encryptionKey; }
    public void setEncryptionKey(String encryptionKey) { this.encryptionKey = encryptionKey; }
    
    public boolean isEnableAutomaticCleanup() { return enableAutomaticCleanup; }
    public void setEnableAutomaticCleanup(boolean enableAutomaticCleanup) { this.enableAutomaticCleanup = enableAutomaticCleanup; }
    
    public int getRetentionDays() { return retentionDays; }
    public void setRetentionDays(int retentionDays) { this.retentionDays = retentionDays; }
    
    public String getCleanupSchedule() { return cleanupSchedule; }
    public void setCleanupSchedule(String cleanupSchedule) { this.cleanupSchedule = cleanupSchedule; }
    
    // Convenience methods
    public boolean shouldCreateSchema() {
        return schemaAction == SchemaAction.CREATE || schemaAction == SchemaAction.CREATE_DROP;
    }
    
    public boolean shouldDropSchema() {
        return schemaAction == SchemaAction.CREATE || schemaAction == SchemaAction.CREATE_DROP;
    }
    
    public boolean shouldUpdateSchema() {
        return schemaAction == SchemaAction.UPDATE;
    }
    
    public boolean shouldValidateSchema() {
        return schemaAction == SchemaAction.VALIDATE || validateSchema;
    }
    
    @Override
    public String toString() {
        return String.format(
            "DatabaseConfiguration{name='%s', schema=%s, persistent=%s, pool=%s, migrations=%s}",
            databaseName, schemaAction, persistentDatabase, useConnectionPool, enableMigrations
        );
    }
    
    /**
     * Create a default configuration for development
     */
    public static DatabaseConfiguration forDevelopment() {
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDatabaseName("logging-dev");
        config.setSchemaAction(SchemaAction.CREATE_DROP);
        config.setShowSql(true);
        config.setFormatSql(true);
        config.setPersistentDatabase(false); // In-memory for development
        config.setEnableMigrations(false); // Skip migrations in dev
        return config;
    }
    
    /**
     * Create a default configuration for production
     */
    public static DatabaseConfiguration forProduction() {
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDatabaseName("logging-prod");
        config.setSchemaAction(SchemaAction.UPDATE);
        config.setShowSql(false);
        config.setPersistentDatabase(true);
        config.setUseConnectionPool(true);
        config.setEnableAutomaticCleanup(true);
        config.setRetentionDays(90); // 3 months retention
        return config;
    }
    
    /**
     * Create a default configuration for testing
     */
    public static DatabaseConfiguration forTesting() {
        DatabaseConfiguration config = new DatabaseConfiguration();
        config.setDatabaseName("logging-test");
        config.setSchemaAction(SchemaAction.CREATE_DROP);
        config.setPersistentDatabase(false); // In-memory for tests
        config.setUseConnectionPool(false); // Simpler for tests
        config.setEnableMigrations(false);
        config.setEnableAutomaticCleanup(false);
        return config;
    }
}
