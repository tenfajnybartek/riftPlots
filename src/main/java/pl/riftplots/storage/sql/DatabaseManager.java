package pl.riftplots.storage.sql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import pl.riftplots.config.StorageConfiguration;
import pl.riftplots.storage.StorageType;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private final StorageConfiguration storageConfig;
    private final StorageType storageType;
    private final File dataFolder;
    private HikariDataSource dataSource;

    public DatabaseManager(StorageConfiguration storageConfig, StorageType storageType, File dataFolder) {
        this.storageConfig = storageConfig;
        this.storageType = storageType;
        this.dataFolder = dataFolder;
    }

    public void initialize() throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();

        if (storageType == StorageType.SQLITE) {
            File dbDir = new File(dataFolder, "data");
            if (!dbDir.exists()) {
                dbDir.mkdirs();
            }
            File dbFile = new File(dbDir, storageConfig.getSqliteFileName());
            hikariConfig.setPoolName("RiftPlots-SQLite-Pool");
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + dbFile.getAbsolutePath());
            hikariConfig.setMaximumPoolSize(1);
            hikariConfig.setConnectionTimeout(30000);
            hikariConfig.setIdleTimeout(0);
            hikariConfig.setMaxLifetime(0);
        } else {
            hikariConfig.setPoolName("RiftPlots-MySQL-Pool");
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            String sslParam = storageConfig.isMysqlUseSsl() ? "true" : "false";
            String url = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&allowPublicKeyRetrieval=true&characterEncoding=utf8&serverTimezone=UTC",
                    storageConfig.getMysqlHost(),
                    storageConfig.getMysqlPort(),
                    storageConfig.getMysqlDatabase(),
                    sslParam);
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(storageConfig.getMysqlUsername());
            hikariConfig.setPassword(storageConfig.getMysqlPassword());
            hikariConfig.setMaximumPoolSize(storageConfig.getPoolMaxSize());
            hikariConfig.setMinimumIdle(storageConfig.getPoolMinIdle());
            hikariConfig.setIdleTimeout(storageConfig.getPoolIdleTimeout());
            hikariConfig.setConnectionTimeout(storageConfig.getPoolConnectionTimeout());
            hikariConfig.setMaxLifetime(storageConfig.getPoolMaxLifetime());
        }

        this.dataSource = new HikariDataSource(hikariConfig);
        try {
            createTables();
        } catch (SQLException e) {
            shutdown();
            throw e;
        }
    }

    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE TABLE IF NOT EXISTS rift_schema_version (id INT PRIMARY KEY, version INT NOT NULL)");
            try (var version = stmt.executeQuery("SELECT version FROM rift_schema_version WHERE id = 1")) {
                if (version.next() && version.getInt(1) > 2)
                    throw new SQLException("Magazyn pochodzi z nowszej wersji riftPlots");
            }
            try (var insert = conn.prepareStatement("INSERT INTO rift_schema_version (id, version) SELECT 1, 1 WHERE NOT EXISTS (SELECT 1 FROM rift_schema_version WHERE id = 1)")) {
                insert.executeUpdate();
            }

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_plots (
                            id VARCHAR(64) PRIMARY KEY,
                            name VARCHAR(128) NOT NULL,
                            description TEXT,
                            category VARCHAR(32) NOT NULL,
                            status VARCHAR(32) NOT NULL,
                            world VARCHAR(64) NOT NULL,
                            min_x INT NOT NULL,
                            min_y INT NOT NULL,
                            min_z INT NOT NULL,
                            max_x INT NOT NULL,
                            max_y INT NOT NULL,
                            max_z INT NOT NULL,
                            worldguard_region_id VARCHAR(64) NOT NULL,
                            price DECIMAL(18,2) NOT NULL,
                            base_tax DECIMAL(18,2) NOT NULL,
                            tax_mode VARCHAR(32) NOT NULL,
                            tax_multiplier DOUBLE NOT NULL,
                            tax_enabled BOOLEAN NOT NULL,
                            billing_period_days INT NOT NULL,
                            grace_period_hours INT NOT NULL,
                            owner_uuid VARCHAR(36),
                            owner_name VARCHAR(64),
                            purchase_date BIGINT,
                            next_tax_payment BIGINT,
                            grace_period_expiry BIGINT,
                            advance_periods_paid INT NOT NULL DEFAULT 0,
                            schematic_file VARCHAR(255),
                            auto_reset_on_expire BOOLEAN NOT NULL,
                            refund_percent_on_abandon DOUBLE NOT NULL,
                            created_at BIGINT NOT NULL,
                            updated_at BIGINT NOT NULL,
                            property_type VARCHAR(32) DEFAULT 'RESIDENTIAL',
                            district_id VARCHAR(64),
                            rental_available BOOLEAN DEFAULT FALSE,
                            rental_price DECIMAL(18,2) DEFAULT 0,
                            rental_period_unit VARCHAR(16) DEFAULT 'DAY',
                            rental_deposit DECIMAL(18,2) DEFAULT 0,
                            active_rental_id VARCHAR(64),
                            home_world VARCHAR(64),
                            home_x DOUBLE,
                            home_y DOUBLE,
                            home_z DOUBLE,
                            home_yaw FLOAT,
                            home_pitch FLOAT,
                            resell_price DECIMAL(18,2) DEFAULT 0,
                            resell_available BOOLEAN DEFAULT FALSE,
                            auction_active BOOLEAN DEFAULT FALSE
                        );
                    """);

            tryAddColumn(stmt, "rift_plots", "property_type", "VARCHAR(32) DEFAULT 'RESIDENTIAL'");
            tryAddColumn(stmt, "rift_plots", "district_id", "VARCHAR(64)");
            tryAddColumn(stmt, "rift_plots", "rental_available", "BOOLEAN DEFAULT FALSE");
            tryAddColumn(stmt, "rift_plots", "rental_price", "DECIMAL(18,2) DEFAULT 0");
            tryAddColumn(stmt, "rift_plots", "rental_period_unit", "VARCHAR(16) DEFAULT 'DAY'");
            tryAddColumn(stmt, "rift_plots", "rental_deposit", "DECIMAL(18,2) DEFAULT 0");
            tryAddColumn(stmt, "rift_plots", "active_rental_id", "VARCHAR(64)");
            tryAddColumn(stmt, "rift_plots", "home_world", "VARCHAR(64)");
            tryAddColumn(stmt, "rift_plots", "home_x", "DOUBLE");
            tryAddColumn(stmt, "rift_plots", "home_y", "DOUBLE");
            tryAddColumn(stmt, "rift_plots", "home_z", "DOUBLE");
            tryAddColumn(stmt, "rift_plots", "home_yaw", "FLOAT");
            tryAddColumn(stmt, "rift_plots", "home_pitch", "FLOAT");
            tryAddColumn(stmt, "rift_plots", "resell_price", "DECIMAL(18,2) DEFAULT 0");
            tryAddColumn(stmt, "rift_plots", "resell_available", "BOOLEAN DEFAULT FALSE");
            tryAddColumn(stmt, "rift_plots", "auction_active", "BOOLEAN DEFAULT FALSE");
            tryAddColumn(stmt, "rift_plots", "settings_overrides", "TEXT");

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_rentals (
                            id VARCHAR(64) PRIMARY KEY,
                            plot_id VARCHAR(64) NOT NULL,
                            owner_uuid VARCHAR(36),
                            tenant_uuid VARCHAR(36) NOT NULL,
                            tenant_name VARCHAR(64) NOT NULL,
                            rent_price DECIMAL(18,2) NOT NULL,
                            period_unit VARCHAR(16) NOT NULL,
                            deposit DECIMAL(18,2) NOT NULL,
                            start_date BIGINT NOT NULL,
                            end_date BIGINT NOT NULL,
                            auto_renew BOOLEAN NOT NULL,
                            status VARCHAR(32) NOT NULL
                        );
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_transfers (
                            id VARCHAR(64) PRIMARY KEY,
                            plot_id VARCHAR(64) NOT NULL,
                            seller_uuid VARCHAR(36) NOT NULL,
                            seller_name VARCHAR(64) NOT NULL,
                            buyer_uuid VARCHAR(36) NOT NULL,
                            buyer_name VARCHAR(64) NOT NULL,
                            price DECIMAL(18,2) NOT NULL,
                            created_at BIGINT NOT NULL,
                            expires_at BIGINT NOT NULL,
                            status VARCHAR(32) NOT NULL
                        );
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_auctions (
                            id VARCHAR(64) PRIMARY KEY,
                            plot_id VARCHAR(64) NOT NULL,
                            seller_uuid VARCHAR(36),
                            starting_price DECIMAL(18,2) NOT NULL,
                            min_bid_increment DECIMAL(18,2) NOT NULL,
                            current_bid DECIMAL(18,2) NOT NULL,
                            current_bidder_uuid VARCHAR(36),
                            current_bidder_name VARCHAR(64),
                            start_time BIGINT NOT NULL,
                            end_time BIGINT NOT NULL,
                            buyout_price DECIMAL(18,2),
                            status VARCHAR(32) NOT NULL
                        );
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_auction_bids (
                            auction_id VARCHAR(64) NOT NULL,
                            bidder_uuid VARCHAR(36) NOT NULL,
                            bidder_name VARCHAR(64) NOT NULL,
                            amount DECIMAL(18,2) NOT NULL,
                            timestamp BIGINT NOT NULL
                        );
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_city_budget (
                            id VARCHAR(64) PRIMARY KEY,
                            type VARCHAR(32) NOT NULL,
                            amount DECIMAL(18,2) NOT NULL,
                            balance_after DECIMAL(18,2) NOT NULL,
                            timestamp BIGINT NOT NULL,
                            plot_id VARCHAR(64),
                            player_uuid VARCHAR(36),
                            description TEXT
                        );
                    """);
            tryAddColumn(stmt, "rift_auctions", "escrow_enabled", "BOOLEAN DEFAULT FALSE");
            tryAddColumn(stmt, "rift_auction_bids", "sequence", "INT NOT NULL DEFAULT 0");

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_audit_logs (
                            id VARCHAR(64) PRIMARY KEY,
                            plot_id VARCHAR(64) NOT NULL,
                            operator_uuid VARCHAR(36),
                            operator_name VARCHAR(64) NOT NULL,
                            action VARCHAR(64) NOT NULL,
                            details TEXT,
                            timestamp BIGINT NOT NULL
                        );
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_plot_members (
                            plot_id VARCHAR(64) NOT NULL,
                            player_uuid VARCHAR(36) NOT NULL,
                            player_name VARCHAR(64) NOT NULL,
                            role VARCHAR(32) NOT NULL,
                            permissions TEXT NOT NULL,
                            added_at BIGINT NOT NULL,
                            PRIMARY KEY (plot_id, player_uuid)
                        );
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_transactions (
                            id VARCHAR(64) PRIMARY KEY,
                            plot_id VARCHAR(64) NOT NULL,
                            player_uuid VARCHAR(36) NOT NULL,
                            player_name VARCHAR(64) NOT NULL,
                            type VARCHAR(32) NOT NULL,
                            amount DECIMAL(18,2) NOT NULL,
                            status VARCHAR(32) NOT NULL,
                            timestamp BIGINT NOT NULL,
                            details TEXT
                        );
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_player_profiles (
                            uuid VARCHAR(36) PRIMARY KEY,
                            last_name VARCHAR(64) NOT NULL,
                            first_join BIGINT NOT NULL,
                            last_seen BIGINT NOT NULL,
                            owned_plots TEXT
                        );
                    """);

            stmt.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS rift_plot_signs (
                            id VARCHAR(64) PRIMARY KEY,
                            plot_id VARCHAR(64) NOT NULL,
                            type VARCHAR(32) NOT NULL,
                            world VARCHAR(64) NOT NULL,
                            x INT NOT NULL,
                            y INT NOT NULL,
                            z INT NOT NULL
                        );
                    """);
            ensureIndex(stmt, "rift_transactions", "idx_rift_tx_plot_time", "plot_id, timestamp");
            ensureIndex(stmt, "rift_transactions", "idx_rift_tx_player_time", "player_uuid, timestamp");
            ensureIndex(stmt, "rift_audit_logs", "idx_rift_audit_plot_time", "plot_id, timestamp");
            ensureIndex(stmt, "rift_auction_bids", "idx_rift_bids_sequence", "auction_id, sequence");
            ensureIndex(stmt, "rift_plots", "idx_rift_plot_owner", "owner_uuid");
            stmt.executeUpdate("UPDATE rift_schema_version SET version = 2 WHERE id = 1");
        }
    }

    private void tryAddColumn(Statement stmt, String table, String column, String definition) throws SQLException {
        try (var columns = stmt.getConnection().getMetaData().getColumns(stmt.getConnection().getCatalog(), null, table, column)) {
            if (columns.next()) return;
        }
        stmt.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
    }

    private void ensureIndex(Statement statement, String table, String name, String columns) throws SQLException {
        try (var indexes = statement.getConnection().getMetaData().getIndexInfo(statement.getConnection().getCatalog(), null, table, false, false)) {
            while (indexes.next()) if (name.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) return;
        }
        statement.executeUpdate("CREATE INDEX " + name + " ON " + table + " (" + columns + ")");
    }

    public int getSchemaVersion() throws SQLException {
        try (var connection = getConnection(); var statement = connection.prepareStatement("SELECT version FROM rift_schema_version WHERE id = 1"); var result = statement.executeQuery()) {
            if (!result.next()) throw new SQLException("Missing schema version");
            return result.getInt(1);
        }
    }

    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("HikariDataSource is closed or not initialized!");
        }
        return dataSource.getConnection();
    }

    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public StorageType getStorageType() {
        return storageType;
    }
}
