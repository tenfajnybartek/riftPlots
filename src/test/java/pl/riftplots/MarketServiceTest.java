package pl.riftplots;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.auction.AuctionStatus;
import pl.riftplots.domain.city.CityTransactionType;
import pl.riftplots.domain.plot.*;
import pl.riftplots.domain.rental.*;
import pl.riftplots.domain.type.PropertyTypeManager;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.service.auction.AuctionService;
import pl.riftplots.service.audit.AuditService;
import pl.riftplots.service.city.CityBudgetService;
import pl.riftplots.service.district.DistrictManager;
import pl.riftplots.service.notification.NotificationService;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.plot.PlotValidationResult;
import pl.riftplots.service.rental.RentalService;
import pl.riftplots.service.reset.PlotResetService;
import pl.riftplots.service.transfer.PlotTransferService;
import pl.riftplots.storage.repository.PlayerProfileRepository;
import pl.riftplots.storage.repository.TransactionRepository;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MarketServiceTest {
    @TempDir
    Path directory;
    PluginConfiguration config;
    PlotManager plots;
    VaultHook vault;
    Player buyer;
    Player seller;
    CityBudgetService budget;
    NotificationService notifications;
    AuditService audit;

    @BeforeEach
    void setup() {
        config = TestFixtures.config();
        plots = mock(PlotManager.class);
        vault = mock(VaultHook.class);
        buyer = player("Buyer");
        seller = player("Seller");
        budget = mock(CityBudgetService.class);
        notifications = mock(NotificationService.class);
        audit = mock(AuditService.class);
        when(plots.validateAcquisition(any(), any())).thenReturn(PlotValidationResult.ok());
        when(vault.isAvailable()).thenReturn(true);
        when(vault.has(any(), any())).thenReturn(true);
        when(vault.withdraw(any(), any())).thenReturn(true);
        when(vault.deposit(any(), any())).thenReturn(true);
    }

    private Player player(String name) {
        Player player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.getName()).thenReturn(name);
        return player;
    }

    private AuctionService auctions() {
        return new AuctionService(directory.toFile(), null, plots, vault, notifications, budget, audit, config, TestFixtures.messages(config));
    }

    private PlotTransferService transfers() {
        return new PlotTransferService(plots, vault, notifications, budget, audit, mock(TransactionRepository.class), config, TestFixtures.messages(config));
    }

    private RentalService rentals() {
        PropertyTypeManager types = mock(PropertyTypeManager.class);
        var type = mock(pl.riftplots.domain.type.PropertyType.class);
        when(type.isRentalAllowed()).thenReturn(true);
        when(types.getType(any())).thenReturn(type);
        return new RentalService(directory.toFile(), null, plots, vault, types, mock(DistrictManager.class), notifications, budget, config, TestFixtures.messages(config));
    }

    private PaymentService payments() {
        return new PaymentService(config, TestFixtures.messages(config), vault, plots, new TaxCalculator(),
                mock(TransactionRepository.class), mock(PlayerProfileRepository.class), mock(PlotResetService.class));
    }

    @Test
    void eventDiscountIsAppliedBeforeBalanceChecksForPurchaseAndTax() {
        var plot = TestFixtures.plot(null);
        when(vault.has(any(), any())).thenAnswer(call -> ((BigDecimal) call.getArgument(1)).compareTo(BigDecimal.ONE) <= 0);
        var events = mock(PluginManager.class);
        doAnswer(call -> {
            Object event = call.getArgument(0);
            if (event instanceof pl.riftplots.api.PlotBuyEvent purchase) purchase.setPrice(BigDecimal.ONE);
            if (event instanceof pl.riftplots.api.PlotTaxPayEvent tax) tax.setAmount(BigDecimal.ONE);
            return null;
        }).when(events).callEvent(any());
        var service = payments();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(events);
            assertTrue(service.buyPlot(buyer, plot));
            assertTrue(service.payTax(buyer, plot, 1));
        }
        verify(vault, times(2)).withdraw(buyer, BigDecimal.ONE);
    }

    @Test
    void failedAuctionWithdrawalNeverTransfersOwnershipOrCreditsTheCity() {
        Plot plot = TestFixtures.plot(null);
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        AuctionService auctions = auctions();
        assertTrue(auctions.startAuction(plot, null, BigDecimal.TEN, BigDecimal.ONE, 5, null));
        assertTrue(auctions.placeBid(buyer, plot.getId(), BigDecimal.TEN));
        var auction = auctions.getAuction(plot.getId()).orElseThrow();
        auction.setEndTime(Instant.now().minusSeconds(1));
        when(vault.withdraw(any(), any())).thenReturn(false);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(buyer.getUniqueId())).thenReturn(buyer);
            auctions.checkCompletedAuctions();
        }
        assertNull(plot.getOwnerUuid());
        assertFalse(plot.isAuctionActive());
        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
        verifyNoInteractions(budget);
    }

    @Test
    void auctionWinnerGetsATaxDeadlineAndOldMembersAreRemoved() {
        Plot plot = TestFixtures.plot(seller.getUniqueId());
        plot.addMember(PlotMember.defaultMember(UUID.randomUUID(), "Friend"));
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        AuctionService auctions = auctions();
        assertTrue(auctions.startAuction(plot, seller.getUniqueId(), BigDecimal.TEN, BigDecimal.ONE, 5, null));
        assertTrue(auctions.placeBid(buyer, plot.getId(), BigDecimal.TEN));
        var auction = auctions.getAuction(plot.getId()).orElseThrow();
        auction.setEndTime(Instant.now().minusSeconds(1));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(buyer.getUniqueId())).thenReturn(buyer);
            bukkit.when(() -> Bukkit.getOfflinePlayer(seller.getUniqueId())).thenReturn(seller);
            auctions.checkCompletedAuctions();
            auctions.checkCompletedAuctions();
        }
        assertEquals(buyer.getUniqueId(), plot.getOwnerUuid());
        assertTrue(plot.getNextTaxPayment().isAfter(Instant.now()));
        assertTrue(plot.getMembers().isEmpty());
        verify(vault, times(1)).withdraw(buyer, BigDecimal.TEN);
        verify(vault).deposit(seller, new BigDecimal("9.50"));
    }

    @Test
    void startAuctionRejectsInvalidParametersAndConflictingRental() {
        AuctionService auctions = auctions();
        Plot plot = TestFixtures.plot(null);
        assertFalse(auctions.startAuction(plot, null, BigDecimal.ONE.negate(), BigDecimal.ONE, 1, null));
        assertFalse(auctions.startAuction(plot, null, BigDecimal.ONE, BigDecimal.ZERO, 1, null));
        assertFalse(auctions.startAuction(plot, null, BigDecimal.ONE, BigDecimal.ONE, 0, null));
        plot.setRentalAvailable(true);
        assertFalse(auctions.startAuction(plot, null, BigDecimal.ONE, BigDecimal.ONE, 1, null));
    }

    @Test
    void buyoutFinalizesWithoutWaitingForTheScheduler() {
        Plot plot = TestFixtures.plot(null);
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        AuctionService auctions = auctions();
        assertTrue(auctions.startAuction(plot, null, BigDecimal.TEN, BigDecimal.ONE, 5, BigDecimal.valueOf(20)));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(buyer.getUniqueId())).thenReturn(buyer);
            assertTrue(auctions.placeBid(buyer, plot.getId(), BigDecimal.valueOf(20)));
        }
        assertEquals(buyer.getUniqueId(), plot.getOwnerUuid());
        assertTrue(auctions.getActiveAuctions().isEmpty());
    }

    @Test
    void changedPlotStatePreventsAcceptanceOfAnOldTransferOffer() {
        Plot plot = TestFixtures.plot(seller.getUniqueId());
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        PlotTransferService transfers = transfers();
        var offer = transfers.createOffer(plot, seller, buyer, BigDecimal.TEN, 300).orElseThrow();
        plot.setStatus(PlotStatus.GRACE_PERIOD);
        assertFalse(transfers.acceptOffer(buyer, offer.getId()));
        assertEquals(seller.getUniqueId(), plot.getOwnerUuid());
        verify(vault, never()).withdraw(any(), any());
    }

    @Test
    void recipientCanRejectTransferOffer() {
        PlotTransferService transfers = transfers();
        Plot plot = TestFixtures.plot(seller.getUniqueId());
        var offer = transfers.createOffer(plot, seller, buyer, BigDecimal.TEN, 300).orElseThrow();
        transfers.cancelOffer(buyer, offer.getId());
        assertTrue(transfers.getOffersForBuyer(buyer.getUniqueId()).isEmpty());
    }

    @Test
    void sellerPayoutFailureRefundsBuyerAndLeavesOwnershipUnchanged() {
        Plot plot = TestFixtures.plot(seller.getUniqueId());
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        when(vault.deposit(eq(seller), any())).thenReturn(false);
        PlotTransferService transfers = transfers();
        var offer = transfers.createOffer(plot, seller, buyer, BigDecimal.TEN, 300).orElseThrow();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(seller.getUniqueId())).thenReturn(seller);
            assertFalse(transfers.acceptOffer(buyer, offer.getId()));
        }
        verify(vault).deposit(buyer, BigDecimal.TEN);
        assertEquals(seller.getUniqueId(), plot.getOwnerUuid());
        verifyNoInteractions(budget);
    }

    @Test
    void rentalExpiredDuringDowntimeIsLoadedAndDepositIsReturnedOnlyOnce() {
        RentalService rentals = rentals();
        Plot plot = TestFixtures.plot(null);
        plot.setActiveRentalId("agreement");
        plot.addMember(PlotMember.defaultMember(buyer.getUniqueId(), buyer.getName()));
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        rentals.saveAgreement(new RentalAgreement("agreement", plot.getId(), null, buyer.getUniqueId(), buyer.getName(),
                BigDecimal.TEN, RentalPeriodUnit.DAY, BigDecimal.valueOf(50), Instant.now().minusSeconds(100),
                Instant.now().minusSeconds(1), false, RentalStatus.ACTIVE));
        RentalService restarted = rentals();
        restarted.initialize();
        assertTrue(restarted.getActiveRental(plot.getId()).isPresent());
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(buyer.getUniqueId())).thenReturn(buyer);
            restarted.checkExpiredRentals();
            restarted.checkExpiredRentals();
        }
        assertFalse(plot.isRented());
        assertFalse(plot.isMember(buyer.getUniqueId()));
        verify(vault, times(1)).deposit(buyer, BigDecimal.valueOf(50));
    }

    @Test
    void failedRenewalPaymentDoesNotExtendRental() {
        RentalService rentals = rentals();
        Plot plot = TestFixtures.plot(null);
        plot.setRentalAvailable(true);
        plot.setActiveRentalId("agreement");
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        var agreement = new RentalAgreement("agreement", plot.getId(), null, buyer.getUniqueId(), buyer.getName(),
                BigDecimal.TEN, RentalPeriodUnit.DAY, BigDecimal.ZERO, Instant.now().minusSeconds(100),
                Instant.now().minusSeconds(1), true, RentalStatus.ACTIVE);
        rentals.saveAgreement(agreement);
        when(vault.withdraw(any(), any())).thenReturn(false);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(buyer.getUniqueId())).thenReturn(buyer);
            rentals.checkExpiredRentals();
        }
        assertEquals(RentalStatus.TERMINATED, agreement.getStatus());
        verifyNoInteractions(budget);
    }

    @Test
    void renewalPaysTheLandlord() {
        RentalService rentals = rentals();
        Plot plot = TestFixtures.plot(seller.getUniqueId());
        plot.setRentalAvailable(true);
        plot.setActiveRentalId("agreement");
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        var agreement = new RentalAgreement("agreement", plot.getId(), seller.getUniqueId(), buyer.getUniqueId(), buyer.getName(),
                BigDecimal.TEN, RentalPeriodUnit.DAY, BigDecimal.ZERO, Instant.now().minusSeconds(100),
                Instant.now().minusSeconds(1), true, RentalStatus.ACTIVE);
        rentals.saveAgreement(agreement);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(buyer.getUniqueId())).thenReturn(buyer);
            bukkit.when(() -> Bukkit.getOfflinePlayer(seller.getUniqueId())).thenReturn(seller);
            rentals.checkExpiredRentals();
        }
        verify(vault).deposit(seller, BigDecimal.TEN);
        assertTrue(agreement.getEndDate().isAfter(Instant.now()));
    }

    @Test
    void directPurchaseCannotBypassAuctionOrRental() {
        Plot plot = TestFixtures.plot(null);
        plot.setAuctionActive(true);
        assertFalse(payments().buyPlot(buyer, plot));
        plot.setAuctionActive(false);
        plot.setRentalAvailable(true);
        assertFalse(payments().buyPlot(buyer, plot));
        verify(vault, never()).withdraw(any(), any());
    }

    @Test
    void auctionPayoutDoesNotCreateMoneyForAFractionalCentBid() {
        Plot plot = TestFixtures.plot(seller.getUniqueId());
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        AuctionService auctions = auctions();
        BigDecimal price = new BigDecimal("0.009");
        assertTrue(auctions.startAuction(plot, seller.getUniqueId(), BigDecimal.ZERO, new BigDecimal("0.001"), 5, null));
        assertTrue(auctions.placeBid(buyer, plot.getId(), price));
        auctions.getAuction(plot.getId()).orElseThrow().setEndTime(Instant.now().minusSeconds(1));
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(buyer.getUniqueId())).thenReturn(buyer);
            bukkit.when(() -> Bukkit.getOfflinePlayer(seller.getUniqueId())).thenReturn(seller);
            auctions.checkCompletedAuctions();
        }
        verify(vault).deposit(seller, price);
        UUID buyerUuid = buyer.getUniqueId();
        verify(budget).recordTransaction(eq(CityTransactionType.AUCTION_FEE), eq(new BigDecimal("0.00")),
                eq(plot.getId()), eq(buyerUuid), anyString());
    }

    @Test
    void transferCommissionCannotExceedThePrice() {
        var settings = TestFixtures.resource("config.yml");
        settings.set("market.transfer-fee-percent", 100);
        config.load(settings);
        Plot plot = TestFixtures.plot(seller.getUniqueId());
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        PlotTransferService transfers = transfers();
        BigDecimal price = new BigDecimal("0.009");
        var offer = transfers.createOffer(plot, seller, buyer, price, 300).orElseThrow();
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(seller.getUniqueId())).thenReturn(seller);
            assertTrue(transfers.acceptOffer(buyer, offer.getId()));
        }
        verify(vault).deposit(eq(seller), argThat(value -> value.signum() == 0));
        verify(budget).recordTransaction(eq(CityTransactionType.PURCHASE_INCOME), eq(price), eq(plot.getId()), any(), anyString());
    }

    @Test
    void escrowChargesOnlyTheIncrementAndNeverChargesTheWinnerAgain() {
        var settings = TestFixtures.resource("config.yml");
        settings.set("auction.escrow-enabled", true);
        config.load(settings);
        Plot plot = TestFixtures.plot(seller.getUniqueId());
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        var journal = new pl.riftplots.service.transaction.OperationJournal(directory.toFile());
        journal.initialize();
        var refunds = new pl.riftplots.service.transaction.RefundService(directory.toFile(), journal, vault);
        refunds.initialize();
        refunds.setEligibility(refund -> journal.find(refund.releaseId()).filter(entry -> entry.status() == pl.riftplots.service.transaction.OperationJournal.Status.COMPLETED).isPresent());
        var auctions = auctions();
        auctions.setOperationJournal(journal);
        auctions.setRefundService(refunds);
        Player second = player("Second");
        assertTrue(auctions.startAuction(plot, seller.getUniqueId(), BigDecimal.TEN, BigDecimal.ONE, 5, null));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(buyer.getUniqueId())).thenReturn(buyer);
            bukkit.when(() -> Bukkit.getOfflinePlayer(second.getUniqueId())).thenReturn(second);
            bukkit.when(() -> Bukkit.getOfflinePlayer(seller.getUniqueId())).thenReturn(seller);
            assertTrue(auctions.placeBid(buyer, plot.getId(), BigDecimal.TEN));
            assertTrue(auctions.placeBid(second, plot.getId(), BigDecimal.valueOf(20)));
            assertTrue(auctions.placeBid(second, plot.getId(), BigDecimal.valueOf(30)));
            var restarted = auctions();
            restarted.setOperationJournal(journal);
            restarted.setRefundService(refunds);
            restarted.initialize();
            var loaded = restarted.getAuction(plot.getId()).orElseThrow();
            assertTrue(loaded.isEscrowEnabled());
            assertEquals(3, loaded.getBids().size());
            loaded.setEndTime(Instant.now().minusSeconds(1));
            restarted.checkCompletedAuctions();
            restarted.checkCompletedAuctions();
            assertEquals(second.getUniqueId(), plot.getOwnerUuid());
            assertEquals(3, restarted.getBidHistory(plot.getId(), 0).size());
        }
        verify(vault, times(1)).withdraw(buyer, BigDecimal.TEN);
        verify(vault, times(1)).withdraw(second, BigDecimal.valueOf(20));
        verify(vault, times(1)).withdraw(second, BigDecimal.TEN);
        verify(vault, never()).withdraw(second, BigDecimal.valueOf(30));
        verify(vault, times(1)).deposit(buyer, BigDecimal.TEN);
        verify(vault).deposit(seller, new BigDecimal("28.50"));
        assertTrue(journal.pending().isEmpty());
    }

    @Test
    void transferOfferSurvivesRestartWithTheSameIdAndExpiry() {
        var plot = TestFixtures.plot(seller.getUniqueId());
        var service = transfers();
        service.initialize(directory.toFile());
        var offer = service.createOffer(plot, seller, buyer, BigDecimal.TEN, 300).orElseThrow();
        var restarted = transfers();
        restarted.initialize(directory.toFile());
        var loaded = restarted.getOffersForBuyer(buyer.getUniqueId()).getFirst();
        assertEquals(offer.getId(), loaded.getId());
        assertEquals(offer.getExpiresAt().toEpochMilli(), loaded.getExpiresAt().toEpochMilli());
        restarted.cancelOffer(buyer, loaded.getId());
        var again = transfers();
        again.initialize(directory.toFile());
        assertTrue(again.getOffersForBuyer(buyer.getUniqueId()).isEmpty());
    }

    @Test
    void failedAuctionCreationLeavesADurableBlockUntilTheTwoRecordsAreReconciled() throws Exception {
        var journal = new pl.riftplots.service.transaction.OperationJournal(directory.toFile()); journal.initialize();
        java.nio.file.Files.createDirectories(directory.resolve("data/auctions.yml/occupied"));
        var plot = TestFixtures.plot(seller.getUniqueId());
        var service = auctions(); service.setOperationJournal(journal);
        assertThrows(IllegalStateException.class, () -> service.startAuction(plot, seller.getUniqueId(), BigDecimal.TEN, BigDecimal.ONE, 5, null));
        assertTrue(journal.isBlocked(plot.getId()));
        var restarted = new pl.riftplots.service.transaction.OperationJournal(directory.toFile()); restarted.initialize();
        assertEquals("AUCTION_START", restarted.pending().getFirst().kind());
        assertTrue(restarted.isBlocked(plot.getId())); verifyNoInteractions(vault);
    }

    @Test
    void taxCannotReactivateADisabledPlot() {
        Plot plot = TestFixtures.plot(buyer.getUniqueId());
        plot.setStatus(PlotStatus.DISABLED);
        assertFalse(payments().payTax(buyer, plot, 1));
        verify(vault, never()).withdraw(any(), any());
    }
}
