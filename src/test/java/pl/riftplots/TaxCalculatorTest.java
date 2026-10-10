package pl.riftplots;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.riftplots.domain.plot.*;
import pl.riftplots.service.payment.TaxCalculator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaxCalculatorTest {
    private TaxCalculator taxCalculator;

    @BeforeEach
    void setUp() {
        taxCalculator = new TaxCalculator();
    }

    private Plot createPlot(TaxMode mode, BigDecimal baseTax, double multiplier, boolean enabled, int x1, int z1, int x2, int z2) {
        PlotBounds bounds = new PlotBounds("world", x1, 60, z1, x2, 80, z2);
        return new Plot(
                "test_plot", "Test Plot", "Desc", PlotCategory.RESIDENTIAL, PlotStatus.OWNED,
                bounds, "rg_test", BigDecimal.valueOf(10000), baseTax, mode, multiplier,
                enabled, 7, 6, null, null, Collections.emptyList(), null, null, null, 0,
                null, true, 50.0, Instant.now(), Instant.now()
        );
    }

    @Test
    void testTaxDisabledReturnsZero() {
        Plot plot = createPlot(TaxMode.FIXED, BigDecimal.valueOf(1500), 1.0, false, 0, 0, 10, 10);
        BigDecimal tax = taxCalculator.calculateTax(plot);
        assertEquals(BigDecimal.ZERO, tax);
    }

    @Test
    void testFixedTax() {
        Plot plot = createPlot(TaxMode.FIXED, BigDecimal.valueOf(1500.50), 1.0, true, 0, 0, 10, 10);
        BigDecimal tax = taxCalculator.calculateTax(plot);
        assertEquals(new BigDecimal("1500.50"), tax);
    }

    @Test
    void testMultiplierTax() {
        Plot plot = createPlot(TaxMode.MULTIPLIER, BigDecimal.valueOf(1000), 1.75, true, 0, 0, 10, 10);
        BigDecimal tax = taxCalculator.calculateTax(plot);
        assertEquals(new BigDecimal("1750.00"), tax);
    }

    @Test
    void testPerBlockTax() {
        Plot plot = createPlot(TaxMode.PER_BLOCK, BigDecimal.valueOf(2.50), 1.0, true, 0, 0, 9, 9);
        BigDecimal tax = taxCalculator.calculateTax(plot);
        assertEquals(new BigDecimal("250.00"), tax);
    }

    @Test
    void testPerChunkTax() {
        Plot plot = createPlot(TaxMode.PER_CHUNK, BigDecimal.valueOf(500), 1.0, true, 0, 0, 31, 31);
        BigDecimal tax = taxCalculator.calculateTax(plot);
        assertEquals(new BigDecimal("2000.00"), tax);
    }

    @Test
    void testSizeTiersTax() {
        Plot smallPlot = createPlot(TaxMode.SIZE_TIERS, BigDecimal.valueOf(100), 1.0, true, 0, 0, 9, 9);
        assertEquals(new BigDecimal("100.00"), taxCalculator.calculateTax(smallPlot));

        Plot mediumPlot = createPlot(TaxMode.SIZE_TIERS, BigDecimal.valueOf(100), 1.0, true, 0, 0, 24, 24);
        assertEquals(new BigDecimal("150.00"), taxCalculator.calculateTax(mediumPlot));

        Plot largePlot = createPlot(TaxMode.SIZE_TIERS, BigDecimal.valueOf(100), 1.0, true, 0, 0, 49, 49);
        assertEquals(new BigDecimal("250.00"), taxCalculator.calculateTax(largePlot));
    }
}
