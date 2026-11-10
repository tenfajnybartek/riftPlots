package pl.riftplots;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.config.StorageConfiguration;
import pl.riftplots.domain.city.CityTransactionType;
import pl.riftplots.domain.payment.*;
import pl.riftplots.domain.player.PlotPlayerProfile;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.audit.AuditService;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.storage.StorageType;
import pl.riftplots.storage.migration.DataMigrationService;
import pl.riftplots.storage.sqlite.SqliteStorageBackend;
import pl.riftplots.storage.yaml.YamlStorageBackend;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class StorageMigrationTest {
    @TempDir
    Path directory;

    private StorageConfiguration storage() {
        StorageConfiguration config = new StorageConfiguration();
        config.load(TestFixtures.resource("storage.yml"));
        return config;
    }

    @Test
    void sqliteToYamlAndBackPreservesProfilesRentalsAuctionsBudgetAndAudit() throws Exception {
        Path sourceDirectory = Files.createDirectory(directory.resolve("source"));
        var storage = storage();
        var backend = new SqliteStorageBackend(storage, sourceDirectory.toFile());
        UUID playerUuid = UUID.randomUUID();
        try {
            backend.initialize();
            Plot plot = TestFixtures.plot(playerUuid);
            plot.setActiveRentalId("rental");
            backend.getPlotRepository().save(plot);
            backend.getPlayerProfileRepository().save(new PlotPlayerProfile(playerUuid, "Owner", Instant.now(), Instant.now(), Set.of(plot.getId())));
            backend.getTransactionRepository().save(TransactionRecord.create(plot.getId(), playerUuid, "Owner",
                    TransactionType.TAX, BigDecimal.TEN, TransactionStatus.SUCCESS, "Paid"));
            var budget = new CityBudgetService(sourceDirectory.toFile(), backend.getDatabaseManager(), BigDecimal.valueOf(1234));
            budget.initialize();
            budget.recordTransaction(CityTransactionType.TAX_INCOME, BigDecimal.TEN, plot.getId(), playerUuid, "Tax");
            new AuditService(sourceDirectory.toFile(), backend.getDatabaseManager(), null).log(plot.getId(), playerUuid, "Owner", "TEST", "Persisted");
            try (var connection = backend.getDatabaseManager().getConnection(); var statement = connection.createStatement()) {
                statement.executeUpdate("INSERT INTO rift_rentals VALUES ('rental','test_plot',NULL,'" + playerUuid + "','Tenant',20,'DAY',30,1000,2000,1,'ACTIVE')");
                statement.executeUpdate("INSERT INTO rift_auctions (id,plot_id,seller_uuid,starting_price,min_bid_increment,current_bid,current_bidder_uuid,current_bidder_name,start_time,end_time,buyout_price,status) VALUES ('auction','another_plot',NULL,100,10,120,'" + playerUuid + "','Bidder',1000,2000,500,'ACTIVE')");
            }
        } finally {
            backend.shutdown();
        }

        var report = new DataMigrationService(sourceDirectory.toFile(), storage).migrate(StorageType.SQLITE, StorageType.YAML);
        assertEquals(1, report.plotsMigrated());
        assertEquals(1, report.transactionsMigrated());
        var yamlBackend = new YamlStorageBackend(sourceDirectory.toFile());
        yamlBackend.initialize();
        assertEquals(playerUuid, yamlBackend.getPlotRepository().findById("test_plot").orElseThrow().getOwnerUuid());
        assertTrue(yamlBackend.getPlayerProfileRepository().findById(playerUuid).isPresent());
        YamlConfiguration rentals = YamlConfiguration.loadConfiguration(sourceDirectory.resolve("data/rentals.yml").toFile());
        assertEquals("ACTIVE", rentals.getString("rentals.rental.status"));
        assertTrue(rentals.getBoolean("rentals.rental.autoRenew"));
        var yamlBudget = new CityBudgetService(sourceDirectory.toFile(), null);
        yamlBudget.initialize();
        assertEquals(0, BigDecimal.valueOf(1244).compareTo(yamlBudget.getBalance()));
        assertEquals(1, yamlBudget.getRecentTransactions(10).size());
        assertEquals(1, new AuditService(sourceDirectory.toFile(), null, null).getLogsForPlot("test_plot", 10).size());

        Path targetDirectory = Files.createDirectory(directory.resolve("target"));
        try (var files = Files.walk(sourceDirectory.resolve("data"))) {
            for (Path file : files.filter(path -> path.toString().endsWith(".yml")).toList()) {
                Path target = targetDirectory.resolve(sourceDirectory.relativize(file));
                Files.createDirectories(target.getParent());
                Files.copy(file, target);
            }
        }
        new DataMigrationService(targetDirectory.toFile(), storage).migrate(StorageType.YAML, StorageType.SQLITE);
        var restored = new SqliteStorageBackend(storage, targetDirectory.toFile());
        try {
            restored.initialize();
            assertEquals(1, restored.getPlayerProfileRepository().findAll().size());
            assertEquals("rental", restored.getPlotRepository().findById("test_plot").orElseThrow().getActiveRentalId());
            try (var connection = restored.getDatabaseManager().getConnection(); var statement = connection.createStatement();
                 var rows = statement.executeQuery("SELECT current_bid, buyout_price FROM rift_auctions WHERE id = 'auction'")) {
                assertTrue(rows.next());
                assertEquals(0, BigDecimal.valueOf(120).compareTo(rows.getBigDecimal(1)));
                assertEquals(0, BigDecimal.valueOf(500).compareTo(rows.getBigDecimal(2)));
            }
            var restoredBudget = new CityBudgetService(targetDirectory.toFile(), restored.getDatabaseManager());
            restoredBudget.initialize();
            assertEquals(0, BigDecimal.valueOf(1244).compareTo(restoredBudget.getBalance()));
        } finally {
            restored.shutdown();
        }
    }

    @Test
    void migrationRefusesToOverwriteNonemptyDestination() throws Exception {
        var yaml = new YamlStorageBackend(directory.toFile());
        yaml.initialize();
        yaml.getPlotRepository().save(TestFixtures.plot(null));
        var migration = new DataMigrationService(directory.toFile(), storage());
        assertThrows(IllegalStateException.class, () -> migration.migrate(StorageType.SQLITE, StorageType.YAML));
        assertEquals(1, yaml.getPlotRepository().findAll().size());
    }

    @Test
    void completeAuctionBidSequenceAndEscrowSurviveSqliteYamlRoundTrip() throws Exception {
        var storage = storage();
        var source = new SqliteStorageBackend(storage, directory.toFile()); source.initialize();
        try {
            source.getPlotRepository().save(TestFixtures.plot(null));
            MySqlIntegrationTest.storeAuctionHistory(source, directory);
            MySqlIntegrationTest.assertAuctionHistory(source, directory);
        } finally { source.shutdown(); }
        new DataMigrationService(directory.toFile(), storage).migrate(StorageType.SQLITE, StorageType.YAML);
        var yaml = new YamlStorageBackend(directory.toFile()); yaml.initialize();
        MySqlIntegrationTest.assertAuctionHistory(yaml, directory);
        var targetDirectory = Files.createDirectory(directory.resolve("roundtrip"));
        try (var paths = Files.walk(directory.resolve("data"))) {
            for (var file : paths.filter(path -> path.toString().endsWith(".yml")).toList()) {
                var target = targetDirectory.resolve(directory.relativize(file));
                Files.createDirectories(target.getParent()); Files.copy(file, target);
            }
        }
        new DataMigrationService(targetDirectory.toFile(), storage).migrate(StorageType.YAML, StorageType.SQLITE);
        var target = new SqliteStorageBackend(storage, targetDirectory.toFile()); target.initialize();
        try { MySqlIntegrationTest.assertAuctionHistory(target, targetDirectory); }
        finally { target.shutdown(); }
    }

    @Test
    void yamlBudgetHistorySurvivesRestartAndUsesConfiguredInitialBalance() {
        var budget = new CityBudgetService(directory.toFile(), null, BigDecimal.valueOf(123));
        budget.initialize();
        budget.adjustBalance(BigDecimal.TEN, null, "First");
        budget.adjustBalance(BigDecimal.ONE, null, "Second");
        var restarted = new CityBudgetService(directory.toFile(), null, BigDecimal.ZERO);
        restarted.initialize();
        assertEquals(BigDecimal.valueOf(134), restarted.getBalance());
        assertEquals("Second", restarted.getRecentTransactions(10).getFirst().getDescription());
        assertEquals(2, restarted.getRecentTransactions(10).size());
    }

    @Test
    void sqliteFailureIsReportedInsteadOfReturningAnEmptySuccess() throws Exception {
        var backend = new SqliteStorageBackend(storage(), directory.toFile());
        backend.initialize();
        backend.shutdown();
        assertThrows(IllegalStateException.class, () -> backend.getPlotRepository().findAll());
        assertThrows(IllegalStateException.class, () -> backend.getPlotRepository().save(TestFixtures.plot(null)));
    }

    @Test
    void malformedYamlIsNotSilentlyTreatedAsAnEmptyDatabase() throws Exception {
        var backend = new YamlStorageBackend(directory.toFile());
        backend.initialize();
        Path plotFile = directory.resolve("data/plots/broken.yml");
        Files.writeString(plotFile, "id: [unterminated\n");
        assertThrows(IllegalStateException.class, () -> backend.getPlotRepository().findAll());
        assertEquals("id: [unterminated\n", Files.readString(plotFile));
    }
}
