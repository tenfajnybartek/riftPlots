package pl.riftplots;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.domain.plot.*;
import pl.riftplots.domain.rental.*;
import pl.riftplots.domain.type.PropertyTypeManager;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.service.district.DistrictManager;
import pl.riftplots.service.notification.NotificationService;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.rental.RentalService;
import pl.riftplots.service.transaction.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class RefundQueueTest {
    @TempDir
    Path directory;

    @Test
    void failedDepositRefundEndsAccessAndSurvivesRestart() {
        var journal = new OperationJournal(directory.toFile());
        journal.initialize();
        var vault = mock(VaultHook.class);
        var refunds = new RefundService(directory.toFile(), journal, vault);
        refunds.initialize();
        var plots = mock(PlotManager.class);
        Plot plot = TestFixtures.plot(null);
        UUID tenant = UUID.randomUUID();
        plot.setActiveRentalId("agreement");
        plot.addMember(new PlotMember(tenant, "Tenant", PlotMemberRole.TENANT, java.util.Set.of(PlotPermission.BUILD), null));
        when(plots.getPlot(plot.getId())).thenReturn(java.util.Optional.of(plot));
        var config = TestFixtures.config();
        var rentals = new RentalService(directory.toFile(), null, plots, vault, mock(PropertyTypeManager.class),
                mock(DistrictManager.class), mock(NotificationService.class), mock(CityBudgetService.class), config, TestFixtures.messages(config));
        rentals.setRefundService(refunds);
        refunds.setEligibility(refund -> rentals.getActiveRental(refund.plotId()).isEmpty());
        rentals.saveAgreement(new RentalAgreement("agreement", plot.getId(), null, tenant, "Tenant", BigDecimal.TEN,
                RentalPeriodUnit.DAY, BigDecimal.valueOf(50), Instant.now().minusSeconds(100), Instant.now().minusSeconds(1), false, RentalStatus.ACTIVE));
        OfflinePlayer player = mock(OfflinePlayer.class);
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(tenant)).thenReturn(player);
            assertTrue(rentals.terminateRental(plot.getId(), true, "Expired"));
            assertFalse(plot.isRented());
            assertFalse(plot.isMember(tenant));
            assertTrue(rentals.getActiveRental(plot.getId()).isEmpty());
            assertEquals(RefundService.Status.PENDING, refunds.list(tenant).getFirst().status());

            var restarted = new RefundService(directory.toFile(), journal, vault);
            restarted.initialize();
            when(vault.deposit(player, BigDecimal.valueOf(50))).thenReturn(true);
            String id = restarted.list(tenant).getFirst().id();
            assertTrue(restarted.retry(id));
            assertTrue(restarted.retry(id));
            assertEquals(RefundService.Status.PAID, restarted.list(tenant).getFirst().status());
            verify(vault, times(2)).deposit(player, BigDecimal.valueOf(50)); // one declined attempt, one successful
        }
    }

    @Test
    void unknownRefundIsNotPaidTwiceAfterRestart() {
        var journal = new OperationJournal(directory.toFile());
        journal.initialize();
        var vault = mock(VaultHook.class);
        var refunds = new RefundService(directory.toFile(), journal, vault);
        refunds.initialize();
        UUID playerId = UUID.randomUUID();
        refunds.prepare("deposit:1", "RENTAL_DEPOSIT", "plot", "agreement", playerId, BigDecimal.TEN, "Expired");
        when(vault.deposit(any(), any())).thenThrow(new IllegalStateException("Unknown payout result"));
        String id = refunds.list(playerId).getFirst().id();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(playerId)).thenReturn(mock(OfflinePlayer.class));
            assertThrows(IllegalStateException.class, () -> refunds.retry(id));
            var restartedJournal = new OperationJournal(directory.toFile());
            restartedJournal.initialize();
            var restarted = new RefundService(directory.toFile(), restartedJournal, vault);
            restarted.initialize();
            assertFalse(restarted.retry(id));
            verify(vault, times(1)).deposit(any(), any());
            assertEquals(RefundService.Status.REVIEW_REQUIRED, restarted.list(playerId).getFirst().status());
        }
    }

    @Test
    void manualPaidReconciliationNeverRepeatsAnUnknownPayout() {
        var journal = new OperationJournal(directory.toFile());
        journal.initialize();
        var vault = mock(VaultHook.class);
        var refunds = new RefundService(directory.toFile(), journal, vault);
        refunds.initialize();
        UUID player = UUID.randomUUID();
        refunds.prepare("refund-unknown", "RENTAL_DEPOSIT", "plot", "rental", player, BigDecimal.TEN, "Deposit");
        String id = refunds.list(player).getFirst().id();
        when(vault.deposit(any(), any())).thenThrow(new IllegalStateException("Unknown outcome"));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(player)).thenReturn(mock(OfflinePlayer.class));
            assertThrows(IllegalStateException.class, () -> refunds.retry(id));
            assertFalse(refunds.retry(id));
            refunds.resolve(id, RefundService.Status.PAID, "Admin", "Verified provider ledger: deposited");
            assertTrue(refunds.retry(id));
            verify(vault, times(1)).deposit(any(), any());
            assertEquals(OperationJournal.Status.COMPLETED, journal.latest("refund:" + id).orElseThrow().status());
            var restarted = new RefundService(directory.toFile(), journal, vault);
            restarted.initialize();
            assertEquals(RefundService.Status.PAID, restarted.list(player).getFirst().status());
        }
    }
}
