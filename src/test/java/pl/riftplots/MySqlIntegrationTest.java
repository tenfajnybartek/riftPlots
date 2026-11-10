package pl.riftplots;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.config.StorageConfiguration;
import pl.riftplots.storage.mysql.MysqlStorageBackend;
import pl.riftplots.storage.StorageType;
import pl.riftplots.storage.migration.DataMigrationService;
import pl.riftplots.storage.yaml.YamlStorageBackend;
import pl.riftplots.domain.plot.*;

import java.nio.file.Path;
import java.sql.DriverManager;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named = "RIFTPLOTS_MYSQL_HOST", matches = ".+")
class MySqlIntegrationTest {
    @TempDir
    Path directory;

    @Test
    void actualMySqlPersistsOwnershipRolesOverridesAndMigratesToYaml() throws Exception {
        String host = System.getenv("RIFTPLOTS_MYSQL_HOST");
        int port = Integer.parseInt(System.getenv().getOrDefault("RIFTPLOTS_MYSQL_PORT", "3306"));
        String user = System.getenv().getOrDefault("RIFTPLOTS_MYSQL_USER", "root");
        String password = System.getenv().getOrDefault("RIFTPLOTS_MYSQL_PASSWORD", "");
        String database = "riftplots_integration_" + UUID.randomUUID().toString().replace("-", "");
        if (!database.matches("riftplots_integration_[a-f0-9]{32}"))
            throw new IllegalStateException("Unsafe test database name");
        String url = "jdbc:mysql://" + host + ":" + port + "/?allowPublicKeyRetrieval=true&useSSL=false";
        try (var admin = DriverManager.getConnection(url, user, password); var statement = admin.createStatement()) {
            statement.executeUpdate("CREATE DATABASE " + database);
            try {
                var yaml = TestFixtures.resource("storage.yml");
                yaml.set("storage.type", "MYSQL");
                yaml.set("storage.mysql.host", host);
                yaml.set("storage.mysql.port", port);
                yaml.set("storage.mysql.database", database);
                yaml.set("storage.mysql.username", user);
                yaml.set("storage.mysql.password", password);
                yaml.set("storage.mysql.use-ssl", false);
                StorageConfiguration config = new StorageConfiguration();
                config.load(yaml);
                UUID owner = UUID.randomUUID(), member = UUID.randomUUID();
                var backend = new MysqlStorageBackend(config, directory.toFile());
                backend.initialize();
                try {
                    var plot = TestFixtures.plot(owner);
                    plot.setPrice(new BigDecimal("1234.56"));
                    plot.setSettingOverrides(java.util.Set.of(PropertySetting.PRICE));
                    plot.addMember(PlotMember.defaultMember(member, "Friend"));
                    backend.getPlotRepository().save(plot);
                    storeAuctionHistory(backend, directory);
                } finally {
                    backend.shutdown();
                }
                var restarted = new MysqlStorageBackend(config, directory.toFile());
                restarted.initialize();
                try {
                    var loaded = restarted.getPlotRepository().findById("test_plot").orElseThrow();
                    assertEquals(owner, loaded.getOwnerUuid());
                    assertTrue(loaded.isMember(member));
                    assertEquals(0, new BigDecimal("1234.56").compareTo(loaded.getPrice()));
                    assertEquals(java.util.Set.of(PropertySetting.PRICE), loaded.getSettingOverrides());
                    assertEquals(2, restarted.getDatabaseManager().getSchemaVersion());
                    assertAuctionHistory(restarted, directory);
                } finally {
                    restarted.shutdown();
                }
                var report = new DataMigrationService(directory.toFile(), config).migrate(StorageType.MYSQL, StorageType.YAML);
                assertEquals(1, report.plotsMigrated());
                var target = new YamlStorageBackend(directory.toFile());
                target.initialize();
                var migrated = target.getPlotRepository().findById("test_plot").orElseThrow();
                assertEquals(owner, migrated.getOwnerUuid());
                assertTrue(migrated.isMember(member));
                assertEquals(java.util.Set.of(PropertySetting.PRICE), migrated.getSettingOverrides());
                assertAuctionHistory(target, directory);
            } finally {
                statement.executeUpdate("DROP DATABASE " + database);
            }
        }
    }

    static pl.riftplots.service.auction.AuctionService auctionService(pl.riftplots.storage.StorageBackend backend, Path folder) {
        var regions = org.mockito.Mockito.mock(pl.riftplots.integration.worldguard.WorldGuardHook.class);
        org.mockito.Mockito.when(regions.syncMembers(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        var plots = new pl.riftplots.service.plot.PlotManager(backend.getPlotRepository(), regions); plots.loadAll();
        var config = TestFixtures.config();
        return new pl.riftplots.service.auction.AuctionService(folder.toFile(), backend.getDatabaseManager(), plots,
                org.mockito.Mockito.mock(pl.riftplots.integration.vault.VaultHook.class),
                org.mockito.Mockito.mock(pl.riftplots.service.notification.NotificationService.class),
                org.mockito.Mockito.mock(pl.riftplots.service.city.CityBudgetService.class),
                org.mockito.Mockito.mock(pl.riftplots.service.audit.AuditService.class), config, TestFixtures.messages(config));
    }

    static void storeAuctionHistory(pl.riftplots.storage.StorageBackend backend, Path folder) {
        var auction = pl.riftplots.domain.auction.PlotAuction.create("test_plot", null, BigDecimal.TEN, BigDecimal.ONE, 60, null);
        auction.setEscrowEnabled(true);
        assertTrue(auction.placeBid(UUID.randomUUID(), "First", BigDecimal.valueOf(20), 120));
        assertTrue(auction.placeBid(UUID.randomUUID(), "Second", BigDecimal.valueOf(30), 120));
        var service = auctionService(backend, folder);
        service.saveAuction(auction);
        service.saveAuction(auction); // replacement must preserve sequence without duplicate bid rows
    }

    static void assertAuctionHistory(pl.riftplots.storage.StorageBackend backend, Path folder) {
        var service = auctionService(backend, folder); service.initialize();
        var auction = service.getAuction("test_plot").orElseThrow();
        assertTrue(auction.isEscrowEnabled());
        assertEquals(java.util.List.of("First", "Second"), auction.getBids().stream().map(pl.riftplots.domain.auction.AuctionBid::getBidderName).toList());
        assertEquals(java.util.List.of("Second", "First"), service.getBidHistory("test_plot", 0).stream().map(pl.riftplots.domain.auction.AuctionBid::getBidderName).toList());
    }
}
