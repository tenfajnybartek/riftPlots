package pl.riftplots.config;

import org.bukkit.configuration.file.FileConfiguration;
import pl.riftplots.storage.StorageType;

public class StorageConfiguration {
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
        String typeStr = config.getString("storage.type", "SQLITE");
        this.storageType = StorageType.parse(typeStr);

        this.sqliteFileName = config.getString("storage.sqlite.file", "riftplots.db");

        this.mysqlHost = config.getString("storage.mysql.host", "localhost");
        this.mysqlPort = config.getInt("storage.mysql.port", 3306);
        this.mysqlDatabase = config.getString("storage.mysql.database", "riftplots");
        this.mysqlUsername = config.getString("storage.mysql.username", "root");
        this.mysqlPassword = config.getString("storage.mysql.password", "");
        this.mysqlUseSsl = config.getBoolean("storage.mysql.use-ssl", false);

        this.poolMaxSize = Math.max(1, config.getInt("storage.pool.maximum-pool-size", 10));
        this.poolMinIdle = Math.max(1, config.getInt("storage.pool.minimum-idle", 2));
        this.poolIdleTimeout = Math.max(10000L, config.getLong("storage.pool.idle-timeout-ms", 600000L));
        this.poolConnectionTimeout = Math.max(1000L, config.getLong("storage.pool.connection-timeout-ms", 30000L));
        this.poolMaxLifetime = Math.max(30000L, config.getLong("storage.pool.max-lifetime-ms", 1800000L));
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
