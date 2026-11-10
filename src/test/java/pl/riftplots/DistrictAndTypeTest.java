package pl.riftplots;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.domain.district.District;
import pl.riftplots.domain.plot.*;
import pl.riftplots.domain.type.PropertyTypeManager;
import pl.riftplots.service.district.DistrictManager;
import pl.riftplots.service.payment.TaxCalculator;

import java.io.File;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DistrictAndTypeTest {
    @TempDir
    File tempDir;

    private PropertyTypeManager typeManager;
    private DistrictManager districtManager;
    private TaxCalculator taxCalculator;

    @BeforeEach
    void setUp() {
        typeManager = new PropertyTypeManager(tempDir);
        typeManager.load();

        districtManager = new DistrictManager(tempDir, typeManager);
        districtManager.load();

        taxCalculator = new TaxCalculator(districtManager);
    }

    @Test
    void testDistrictMultiplierInheritance() {
        PlotBounds bounds = new PlotBounds("world", 0, 60, 0, 10, 80, 10);
        Plot plot = new Plot(
                "p_centrum", "Dom Centrum", "Desc", PlotCategory.RESIDENTIAL, PlotStatus.AVAILABLE,
                bounds, "rg_c", BigDecimal.valueOf(10000), BigDecimal.valueOf(1000),
                TaxMode.FIXED, 1.0, true, 7, 6, null, null, Collections.emptyList(),
                null, null, null, 0, null, true, 50.0, Instant.now(), Instant.now()
        );

        plot.setDistrictId("centrum");
        BigDecimal calculatedTax = taxCalculator.calculateTax(plot);

        assertEquals(new BigDecimal("1250.00"), calculatedTax);

        plot.setDistrictId("przedmiescia");
        BigDecimal suburbanTax = taxCalculator.calculateTax(plot);
        assertEquals(new BigDecimal("750.00"), suburbanTax);
    }

    @Test
    void testDistrictAllowedTypes() {
        District industrial = districtManager.getDistrict("przemyslowa").orElseThrow();
        assertTrue(industrial.isTypeAllowed("INDUSTRIAL"));
        assertFalse(industrial.isTypeAllowed("RESIDENTIAL"));
        assertFalse(industrial.isRentalEnabled());
    }

    @Test
    void inheritedPriceUsesDistrictButExplicitPriceIsFinalAndSurvivesYaml() {
        var repository = new pl.riftplots.storage.yaml.YamlPlotRepository(new File(tempDir, "plots"));
        var regions = org.mockito.Mockito.mock(pl.riftplots.integration.worldguard.WorldGuardHook.class);
        org.mockito.Mockito.when(regions.syncMembers(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        var plots = new pl.riftplots.service.plot.PlotManager(repository, regions);
        plots.configureRules(typeManager, districtManager, null);
        var plot = TestFixtures.plot(null);
        plot.setDistrictId("centrum");
        plot.setSettingOverrides(Set.of());
        plots.savePlot(plot);
        assertEquals(0, new BigDecimal("37500").compareTo(plots.getPurchasePrice(plot)));
        assertTrue(repository.findById(plot.getId()).orElseThrow().getSettingOverrides().isEmpty());
        plot.setPrice(BigDecimal.valueOf(1234));
        plots.savePlot(plot);
        assertEquals(0, BigDecimal.valueOf(1234).compareTo(plots.getPurchasePrice(plot)));
        assertEquals(Set.of(PropertySetting.PRICE), repository.findById(plot.getId()).orElseThrow().getSettingOverrides());
    }

    @Test
    void reloadPreservesOwnedBillingUntilAnExplicitApplyDefaults() throws Exception {
        var plot = TestFixtures.plot(UUID.randomUUID());
        plot.setSettingOverrides(Set.of());
        var repository = org.mockito.Mockito.mock(pl.riftplots.storage.repository.PlotRepository.class);
        org.mockito.Mockito.when(repository.findAll()).thenReturn(List.of(plot));
        var regions = org.mockito.Mockito.mock(pl.riftplots.integration.worldguard.WorldGuardHook.class);
        org.mockito.Mockito.when(regions.syncMembers(org.mockito.ArgumentMatchers.any())).thenReturn(true);
        var plots = new pl.riftplots.service.plot.PlotManager(repository, regions);
        plots.loadAll();
        plots.configureRules(typeManager, districtManager, null);
        File file = new File(tempDir, "plot-types.yml");
        var yaml = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
        yaml.set("types.RESIDENTIAL.default-base-tax", 2500);
        yaml.save(file);
        typeManager.load();
        plots.refreshInheritedDefaults();
        assertEquals(0, BigDecimal.valueOf(100).compareTo(plot.getBaseTax()));
        plots.applyTypeDefaults(plot);
        assertEquals(0, BigDecimal.valueOf(2500).compareTo(plot.getBaseTax()));
        assertTrue(plot.getSettingOverrides().isEmpty());
        assertEquals("deny", typeManager.getType("RESIDENTIAL").getDefaultFlags().get("pvp"));
    }
}
