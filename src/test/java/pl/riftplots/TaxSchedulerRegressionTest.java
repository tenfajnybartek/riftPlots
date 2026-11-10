package pl.riftplots;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.scheduler.PlotTaxScheduler;
import pl.riftplots.service.auction.AuctionService;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.rental.RentalService;
import pl.riftplots.service.reset.PlotResetService;
import pl.riftplots.storage.repository.PlayerProfileRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaxSchedulerRegressionTest {
    PlotManager plots;
    PlotResetService resets;
    PlotTaxScheduler scheduler;

    @BeforeEach
    void setup() {
        Plugin plugin = mock(Plugin.class);
        Logger logger = Logger.getAnonymousLogger();
        logger.setLevel(Level.OFF);
        when(plugin.getLogger()).thenReturn(logger);
        plots = mock(PlotManager.class);
        resets = mock(PlotResetService.class);
        var config = TestFixtures.config();
        scheduler = new PlotTaxScheduler(plugin, config, TestFixtures.messages(config), plots,
                new TaxCalculator(), resets, null, mock(PlayerProfileRepository.class));
    }

    @Test
    void missingGraceDeadlineUsesTheOriginalTaxDeadline() {
        Plot plot = TestFixtures.plot(UUID.randomUUID());
        plot.setStatus(PlotStatus.PAYMENT_DUE);
        Instant deadline = Instant.now().minus(Duration.ofDays(2));
        plot.setNextTaxPayment(deadline);
        when(plots.getAllPlots()).thenReturn(List.of(plot));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            scheduler.run();
        }
        assertEquals(deadline.plus(Duration.ofHours(plot.getGracePeriodHours())), plot.getGracePeriodExpiry());
        verify(resets).startReset(eq(plot), any());
    }

    @Test
    void rejectedResetDoesNotAnnounceThatOwnershipWasLost() {
        Plot plot = TestFixtures.plot(UUID.randomUUID());
        plot.setStatus(PlotStatus.GRACE_PERIOD);
        plot.setGracePeriodExpiry(Instant.now().minusSeconds(1));
        when(plots.getAllPlots()).thenReturn(List.of(plot));
        when(resets.startReset(eq(plot), any())).thenReturn(false);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            assertDoesNotThrow(scheduler::run);
            bukkit.verify(Bukkit::getConsoleSender, never());
            bukkit.verify(Bukkit::getOnlinePlayers, never());
        }
        assertNotNull(plot.getOwnerUuid());
    }

    @Test
    void acceptedExpirationWithoutResetReleasesOwnershipAndAnnouncesIt() {
        Plot plot = TestFixtures.plot(UUID.randomUUID());
        plot.setAutoResetOnExpire(false);
        plot.setStatus(PlotStatus.GRACE_PERIOD);
        plot.setGracePeriodExpiry(Instant.now().minusSeconds(1));
        when(plots.getAllPlots()).thenReturn(List.of(plot));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            scheduler.run();
            bukkit.verify(Bukkit::getConsoleSender);
        }
        assertNull(plot.getOwnerUuid());
        assertEquals(PlotStatus.AVAILABLE, plot.getStatus());
        verify(plots).savePlot(plot);
    }

    @Test
    void failingRentalCheckDoesNotStopAuctions() {
        RentalService rentals = mock(RentalService.class);
        AuctionService auctions = mock(AuctionService.class);
        doThrow(new IllegalStateException("Write failed")).when(rentals).checkExpiredRentals();
        scheduler.setRentalService(rentals);
        scheduler.setAuctionService(auctions);
        assertDoesNotThrow(scheduler::run);
        verify(auctions).checkCompletedAuctions();
    }

    @Test
    void oneBrokenPlotDoesNotStopRentalAndAuctionChecks() {
        Plot broken = TestFixtures.plot(UUID.randomUUID());
        broken.setNextTaxPayment(Instant.now().minusSeconds(1));
        when(plots.getAllPlots()).thenReturn(List.of(broken));
        doThrow(new IllegalStateException("Write failed")).when(plots).savePlot(broken);
        RentalService rentals = mock(RentalService.class);
        AuctionService auctions = mock(AuctionService.class);
        scheduler.setRentalService(rentals);
        scheduler.setAuctionService(auctions);
        assertDoesNotThrow(scheduler::run);
        verify(rentals).checkExpiredRentals();
        verify(auctions).checkCompletedAuctions();
    }
}
