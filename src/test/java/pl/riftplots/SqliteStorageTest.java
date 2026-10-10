package pl.riftplots;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.config.StorageConfiguration;
import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.payment.TransactionStatus;
import pl.riftplots.domain.payment.TransactionType;
import pl.riftplots.domain.plot.*;
import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.domain.sign.PlotSignType;
import pl.riftplots.storage.sqlite.SqliteStorageBackend;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class SqliteStorageTest {
    @TempDir
    File tempDir;

    private SqliteStorageBackend backend;

    @BeforeEach
    void setUp() throws Exception {
        YamlConfiguration config = new YamlConfiguration();
        config.set("storage.type", "SQLITE");
        config.set("storage.sqlite.file", "test_riftplots.db");

        StorageConfiguration storageConfig = new StorageConfiguration();
        storageConfig.load(config);

        backend = new SqliteStorageBackend(storageConfig, tempDir);
        backend.initialize();
    }

    @AfterEach
    void tearDown() {
        if (backend != null) {
            backend.shutdown();
        }
    }

    @Test
    void testPlotSaveAndFind() {
        PlotBounds bounds = new PlotBounds("world", 100, 64, 100, 120, 80, 120);
        Plot plot = new Plot(
                "plaza_1", "Plaza Central", "Rynek", PlotCategory.COMMERCIAL, PlotStatus.AVAILABLE,
                bounds, "rg_plaza_1", BigDecimal.valueOf(30000), BigDecimal.valueOf(2000),
                TaxMode.FIXED, 1.0, true, 7, 6, null, null, Collections.emptyList(),
                null, null, null, 0, "plaza_1.schem", true, 50.0, Instant.now(), Instant.now()
        );

        backend.getPlotRepository().save(plot);

        Optional<Plot> loadedOpt = backend.getPlotRepository().findById("plaza_1");
        assertTrue(loadedOpt.isPresent());
        Plot loaded = loadedOpt.get();
        assertEquals("plaza_1", loaded.getId());
        assertEquals("Plaza Central", loaded.getName());
        assertEquals(30000.0, loaded.getPrice().doubleValue());
        assertEquals(100, loaded.getBounds().getMinX());
        assertEquals(120, loaded.getBounds().getMaxX());

        UUID ownerUuid = UUID.randomUUID();
        loaded.setStatus(PlotStatus.OWNED);
        loaded.setOwnerUuid(ownerUuid);
        loaded.setOwnerName("JanKowalski");
        loaded.addMember(PlotMember.defaultMember(UUID.randomUUID(), "Kolega"));

        backend.getPlotRepository().save(loaded);

        Optional<Plot> reloadedOpt = backend.getPlotRepository().findById("plaza_1");
        assertTrue(reloadedOpt.isPresent());
        Plot reloaded = reloadedOpt.get();
        assertEquals(PlotStatus.OWNED, reloaded.getStatus());
        assertEquals(ownerUuid, reloaded.getOwnerUuid());
        assertEquals("JanKowalski", reloaded.getOwnerName());
        assertEquals(1, reloaded.getMembers().size());

        List<Plot> ownedList = backend.getPlotRepository().findByOwner(ownerUuid);
        assertEquals(1, ownedList.size());

        backend.getPlotRepository().delete("plaza_1");
        assertTrue(backend.getPlotRepository().findById("plaza_1").isEmpty());
    }

    @Test
    void testTransactions() {
        UUID playerUuid = UUID.randomUUID();
        TransactionRecord record = TransactionRecord.create(
                "plot_10", playerUuid, "Adam", TransactionType.PURCHASE,
                BigDecimal.valueOf(15000), TransactionStatus.SUCCESS, "Zakup"
        );
        backend.getTransactionRepository().save(record);

        List<TransactionRecord> byPlot = backend.getTransactionRepository().findByPlotId("plot_10");
        assertEquals(1, byPlot.size());
        assertEquals(15000.0, byPlot.get(0).getAmount().doubleValue());

        List<TransactionRecord> byPlayer = backend.getTransactionRepository().findByPlayer(playerUuid);
        assertEquals(1, byPlayer.size());
    }

    @Test
    void testSigns() {
        PlotSign sign = new PlotSign("sign_1", "plot_5", PlotSignType.FOR_SALE, "world", 10, 65, 20);
        backend.getPlotSignRepository().save(sign);

        Optional<PlotSign> loaded = backend.getPlotSignRepository().findById("sign_1");
        assertTrue(loaded.isPresent());
        assertEquals("plot_5", loaded.get().getPlotId());

        Optional<PlotSign> byLoc = backend.getPlotSignRepository().findByLocation("world", 10, 65, 20);
        assertTrue(byLoc.isPresent());

        backend.getPlotSignRepository().delete("sign_1");
        assertTrue(backend.getPlotSignRepository().findById("sign_1").isEmpty());
    }
}
