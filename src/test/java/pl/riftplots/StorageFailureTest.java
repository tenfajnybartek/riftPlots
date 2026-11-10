package pl.riftplots;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.domain.city.CityTransactionType;
import pl.riftplots.integration.worldguard.WorldGuardHook;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.storage.repository.PlotRepository;
import pl.riftplots.storage.sql.DatabaseManager;
import pl.riftplots.storage.yaml.YamlPlotRepository;
import pl.riftplots.storage.yaml.YamlDataFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StorageFailureTest {
    @TempDir
    Path directory;

    @Test
    void partialYamlSerializationLeavesThePreviousFileIntact() throws Exception {
        Path file = directory.resolve("budget.yml");
        String previous = "balance: '100'\n";
        Files.writeString(file, previous);
        YamlConfiguration yaml = mock(YamlConfiguration.class);
        doAnswer(invocation -> {
            File written = invocation.getArgument(0);
            Files.writeString(written.toPath(), "partial");
            throw new IOException("Disk full");
        }).when(yaml).save(any(File.class));

        assertThrows(IOException.class, () -> YamlDataFile.save(yaml, file.toFile()));
        assertEquals(previous, Files.readString(file));
        try (var files = Files.list(directory)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void failedYamlBudgetWriteDoesNotChangeBalanceOrHistory() throws Exception {
        CityBudgetService budget = new CityBudgetService(directory.toFile(), null, BigDecimal.valueOf(100));
        budget.initialize();
        Path file = directory.resolve("data/city_budget.yml");
        Files.delete(file);
        Files.createDirectories(file.resolve("occupied"));

        assertThrows(IllegalStateException.class, () -> budget.recordTransaction(
                CityTransactionType.TAX_INCOME, BigDecimal.TEN, "test_plot", null, "Test"));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(budget.getBalance()));
        assertTrue(budget.getRecentTransactions(10).isEmpty());

        Files.delete(file.resolve("occupied"));
        Files.delete(file);
        budget.recordTransaction(CityTransactionType.TAX_INCOME, BigDecimal.TEN, "test_plot", null, "Retry");
        CityBudgetService restarted = new CityBudgetService(directory.toFile(), null, BigDecimal.ZERO);
        restarted.initialize();
        assertEquals(0, BigDecimal.valueOf(110).compareTo(restarted.getBalance()));
        assertEquals(1, restarted.getRecentTransactions(10).size());
    }

    @Test
    void failedSqlBudgetWriteDoesNotChangeBalance() throws Exception {
        DatabaseManager database = mock(DatabaseManager.class);
        when(database.getConnection()).thenThrow(new SQLException("Database unavailable"));
        CityBudgetService budget = new CityBudgetService(directory.toFile(), database, BigDecimal.valueOf(100));
        assertThrows(IllegalStateException.class, () -> budget.adjustBalance(BigDecimal.TEN, null, "Test"));
        assertEquals(0, BigDecimal.valueOf(100).compareTo(budget.getBalance()));
    }

    @Test
    void failedPlotDeletionKeepsProtectionAndCache() {
        var plot = TestFixtures.plot(UUID.randomUUID());
        PlotRepository repository = mock(PlotRepository.class);
        WorldGuardHook regions = mock(WorldGuardHook.class);
        when(repository.findAll()).thenReturn(List.of(plot));
        PlotManager plots = new PlotManager(repository, regions);
        plots.loadAll();
        doThrow(new IllegalStateException("Write failed")).when(repository).delete(plot.getId());

        assertThrows(IllegalStateException.class, () -> plots.deletePlot(plot.getId()));
        assertSame(plot, plots.getPlot(plot.getId()).orElseThrow());
        verifyNoInteractions(regions);
    }

    @Test
    void failedSaveDoesNotRegisterANewPlotInMemory() {
        var plot = TestFixtures.plot(null);
        PlotRepository repository = mock(PlotRepository.class);
        WorldGuardHook regions = mock(WorldGuardHook.class);
        doThrow(new IllegalStateException("Write failed")).when(repository).save(plot);
        PlotManager plots = new PlotManager(repository, regions);

        assertThrows(IllegalStateException.class, () -> plots.savePlot(plot));
        assertTrue(plots.getPlot(plot.getId()).isEmpty());
        verifyNoInteractions(regions);
    }

    @Test
    void yamlDeletionReportsFailureInsteadOfPretendingSuccess() throws Exception {
        YamlPlotRepository repository = new YamlPlotRepository(directory.toFile());
        Files.createDirectories(directory.resolve("test_plot.yml/occupied"));
        assertThrows(IllegalStateException.class, () -> repository.delete("test_plot"));
    }

    @Test
    void failedExistingPlotWriteRestoresOwnerAndNestedMemberPermissions() {
        var owner = UUID.randomUUID();
        var plot = TestFixtures.plot(owner);
        var member = pl.riftplots.domain.plot.PlotMember.defaultMember(UUID.randomUUID(), "Member");
        plot.addMember(member);
        PlotRepository repository = mock(PlotRepository.class);
        when(repository.findAll()).thenReturn(List.of(plot));
        PlotManager plots = new PlotManager(repository, mock(WorldGuardHook.class));
        plots.loadAll();
        plot.setOwnerUuid(UUID.randomUUID());
        member.grantPermission(pl.riftplots.domain.plot.PlotPermission.BUILD);
        doThrow(new IllegalStateException("Disk full")).when(repository).save(plot);
        assertThrows(IllegalStateException.class, () -> plots.savePlot(plot));
        assertEquals(owner, plot.getOwnerUuid());
        assertFalse(plot.getMember(member.getPlayerUuid()).hasPermission(pl.riftplots.domain.plot.PlotPermission.BUILD));
    }

    @Test
    void protectionSynchronizationFailureIsReportedAfterDurableWrite() {
        var plot = TestFixtures.plot(null);
        PlotRepository repository = mock(PlotRepository.class);
        WorldGuardHook protection = mock(WorldGuardHook.class);
        PlotManager plots = new PlotManager(repository, protection);
        assertThrows(IllegalStateException.class, () -> plots.savePlot(plot));
        verify(repository).save(plot);
        assertSame(plot, plots.getPlot(plot.getId()).orElseThrow());
    }
}
