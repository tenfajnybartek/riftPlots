package pl.riftplots;

import org.junit.jupiter.api.Test;
import pl.riftplots.domain.plot.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlotStateMachineTest {

    @Test
    void testStateTransitions() {
        PlotBounds bounds = new PlotBounds("world", 0, 60, 0, 10, 80, 10);
        Plot plot = new Plot(
                "market_1", "Rynek 1", "Kamienica", PlotCategory.COMMERCIAL, PlotStatus.AVAILABLE,
                bounds, "rg_market_1", BigDecimal.valueOf(25000), BigDecimal.valueOf(1500),
                TaxMode.FIXED, 1.0, true, 7, 6, null, null, Collections.emptyList(),
                null, null, null, 0, "market_1.schem", true, 50.0, Instant.now(), Instant.now()
        );

        assertTrue(plot.getStatus().canBePurchased());
        assertFalse(plot.getStatus().isOwned());

        UUID playerUuid = UUID.randomUUID();
        plot.setStatus(PlotStatus.OWNED);
        plot.setOwnerUuid(playerUuid);
        plot.setOwnerName("Tester");

        assertFalse(plot.getStatus().canBePurchased());
        assertTrue(plot.getStatus().isOwned());
        assertEquals(playerUuid, plot.getOwnerUuid());

        plot.setStatus(PlotStatus.PAYMENT_DUE);
        assertTrue(plot.getStatus().isOwned());

        plot.setStatus(PlotStatus.GRACE_PERIOD);
        assertTrue(plot.getStatus().isOwned());
        assertTrue(plot.getStatus().isInGracePeriod());

        plot.setStatus(PlotStatus.RESETTING);
        assertFalse(plot.getStatus().isOwned());
        assertTrue(plot.getStatus().isResetting());
        assertFalse(plot.getStatus().canBePurchased());

        plot.setStatus(PlotStatus.AVAILABLE);
        plot.setOwnerUuid(null);
        assertTrue(plot.getStatus().canBePurchased());

        plot.setStatus(PlotStatus.DISABLED);
        assertTrue(plot.getStatus().isDisabled());
        assertFalse(plot.getStatus().canBePurchased());
    }
}
