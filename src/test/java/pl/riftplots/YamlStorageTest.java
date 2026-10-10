package pl.riftplots;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.domain.payment.TransactionRecord;
import pl.riftplots.domain.payment.TransactionStatus;
import pl.riftplots.domain.payment.TransactionType;
import pl.riftplots.domain.plot.*;
import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.domain.sign.PlotSignType;
import pl.riftplots.storage.yaml.YamlStorageBackend;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class YamlStorageTest {
    @TempDir
    File tempDir;

    private YamlStorageBackend backend;

    @BeforeEach
    void setUp() {
        backend = new YamlStorageBackend(tempDir);
        backend.initialize();
    }

    @Test
    void testPlotSaveLoadYaml() {
        PlotBounds bounds = new PlotBounds("city_world", 50, 60, 50, 70, 90, 70);
        Plot plot = new Plot(
                "house_1", "Dom Rynek", "Opis", PlotCategory.RESIDENTIAL, PlotStatus.AVAILABLE,
                bounds, "rg_house_1", BigDecimal.valueOf(50000), BigDecimal.valueOf(2500),
                TaxMode.PER_BLOCK, 1.25, true, 14, 12, null, null, Collections.emptyList(),
                null, null, null, 0, "house_1.schem", true, 60.0, Instant.now(), Instant.now()
        );

        backend.getPlotRepository().save(plot);

        Optional<Plot> loadedOpt = backend.getPlotRepository().findById("house_1");
        assertTrue(loadedOpt.isPresent());
        Plot loaded = loadedOpt.get();

        assertEquals("house_1", loaded.getId());
        assertEquals("Dom Rynek", loaded.getName());
        assertEquals(50000.0, loaded.getPrice().doubleValue());
        assertEquals(2500.0, loaded.getBaseTax().doubleValue());
        assertEquals(TaxMode.PER_BLOCK, loaded.getTaxMode());
        assertEquals(1.25, loaded.getTaxMultiplier());
        assertEquals(14, loaded.getBillingPeriodDays());
        assertEquals(12, loaded.getGracePeriodHours());

        UUID owner = UUID.randomUUID();
        loaded.setStatus(PlotStatus.OWNED);
        loaded.setOwnerUuid(owner);
        loaded.setOwnerName("Tester");
        PlotMember member = PlotMember.defaultMember(UUID.randomUUID(), "Member1");
        loaded.addMember(member);
        backend.getPlotRepository().save(loaded);

        Plot reloaded = backend.getPlotRepository().findById("house_1").orElseThrow();
        assertEquals(owner, reloaded.getOwnerUuid());
        assertEquals(1, reloaded.getMembers().size());
        assertTrue(reloaded.isMember(member.getPlayerUuid()));

        backend.getPlotRepository().delete("house_1");
        assertTrue(backend.getPlotRepository().findById("house_1").isEmpty());
    }

    @Test
    void testTransactionsAndSignsYaml() {
        TransactionRecord record = TransactionRecord.create(
                "plot_x", UUID.randomUUID(), "Gracz", TransactionType.TAX,
                BigDecimal.valueOf(100), TransactionStatus.SUCCESS, "Opłata"
        );
        backend.getTransactionRepository().save(record);

        List<TransactionRecord> list = backend.getTransactionRepository().findByPlotId("plot_x");
        assertEquals(1, list.size());

        PlotSign sign = new PlotSign("sign_a", "plot_x", PlotSignType.RENEW, "city_world", 100, 64, 100);
        backend.getPlotSignRepository().save(sign);

        assertTrue(backend.getPlotSignRepository().findById("sign_a").isPresent());
        assertEquals(1, backend.getPlotSignRepository().findByPlotId("plot_x").size());

        backend.getPlotSignRepository().deleteByPlotId("plot_x");
        assertEquals(0, backend.getPlotSignRepository().findByPlotId("plot_x").size());
    }
}
