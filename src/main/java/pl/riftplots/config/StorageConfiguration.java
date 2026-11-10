package pl.riftplots.config;

import org.bukkit.configuration.file.FileConfiguration;
import pl.riftplots.storage.StorageType;

public class StorageConfiguration {
    public StorageConfiguration copy() {
        StorageConfiguration copy = new StorageConfiguration();
        copy.storageType = storageType;
        copy.sqliteFileName = sqliteFileName;
        copy.mysqlHost = mysqlHost;
        copy.mysqlPort = mysqlPort;
        copy.mysqlDatabase = mysqlDatabase;
        copy.mysqlUsername = mysqlUsername;
        copy.mysqlPassword = mysqlPassword;
        copy.mysqlUseSsl = mysqlUseSsl;
        copy.poolMaxSize = poolMaxSize;
        copy.poolMinIdle = poolMinIdle;
        copy.poolIdleTimeout = poolIdleTimeout;
        copy.poolConnectionTimeout = poolConnectionTimeout;
        copy.poolMaxLifetime = poolMaxLifetime;
        return copy;
    }

    private StorageType storageType;

    private String sqliteFileName;

    private String mysqlHost;
    private int mysqlPort;
    private String mysqlDatabase;
    private String mysqlUsername;
    private String mysqlPassword;
    private boolean mysqlUseSsl;

    private int poolMaxSize;
    private int poolMinIdle;
    private long poolIdleTimeout;
    private long poolConnectionTimeout;
    private long poolMaxLifetime;

    public void load(FileConfiguration config) {
        config = ConfigurationFiles.merged("storage.yml", config);
        String typeStr = config.getString("storage.type");
        this.storageType = StorageType.parse(typeStr);

        this.sqliteFileName = config.getString("storage.sqlite.file");

        this.mysqlHost = config.getString("storage.mysql.host");
        this.mysqlPort = config.getInt("storage.mysql.port");
        this.mysqlDatabase = config.getString("storage.mysql.database");
        this.mysqlUsername = config.getString("storage.mysql.username");
        this.mysqlPassword = config.getString("storage.mysql.password");
        this.mysqlUseSsl = config.getBoolean("storage.mysql.use-ssl");

        this.poolMaxSize = Math.max(1, config.getInt("storage.pool.maximum-pool-size"));
        this.poolMinIdle = Math.max(1, config.getInt("storage.pool.minimum-idle"));
        this.poolIdleTimeout = Math.max(10000L, config.getLong("storage.pool.idle-timeout-ms"));
        this.poolConnectionTimeout = Math.max(1000L, config.getLong("storage.pool.connection-timeout-ms"));
        this.poolMaxLifetime = Math.max(30000L, config.getLong("storage.pool.max-lifetime-ms"));
    }

    public StorageType getStorageType() {
        return storageType;
    }

    public String getSqliteFileName() {
        return sqliteFileName;
    }

    public String getMysqlHost() {
        return mysqlHost;
    }

    public int getMysqlPort() {
        return mysqlPort;
    }

    public String getMysqlDatabase() {
        return mysqlDatabase;
    }

    public String getMysqlUsername() {
        return mysqlUsername;
    }

    public String getMysqlPassword() {
        return mysqlPassword;
    }

    public boolean isMysqlUseSsl() {
        return mysqlUseSsl;
    }

    public int getPoolMaxSize() {
        return poolMaxSize;
    }

    public int getPoolMinIdle() {
        return poolMinIdle;
    }

    public long getPoolIdleTimeout() {
        return poolIdleTimeout;
    }

    public long getPoolConnectionTimeout() {
        return poolConnectionTimeout;
    }

    public long getPoolMaxLifetime() {
        return poolMaxLifetime;
    }
}
