package pl.riftplots;

import org.junit.jupiter.api.Test;
import pl.riftplots.domain.auction.AuctionStatus;
import pl.riftplots.domain.auction.PlotAuction;
import pl.riftplots.domain.rental.RentalAgreement;
import pl.riftplots.domain.rental.RentalPeriodUnit;
import pl.riftplots.domain.rental.RentalStatus;
import pl.riftplots.domain.transfer.TransferOffer;
import pl.riftplots.domain.transfer.TransferStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RentalAndAuctionTest {

    @Test
    void testRentalAgreementCalculation() {
        UUID owner = UUID.randomUUID();
        UUID tenant = UUID.randomUUID();

        RentalAgreement agreement = RentalAgreement.create(
                "plot_1", owner, tenant, "Najemca",
                BigDecimal.valueOf(500), RentalPeriodUnit.DAY, BigDecimal.valueOf(1000), 3, true
        );

        assertTrue(agreement.isActive());
        assertEquals(RentalStatus.ACTIVE, agreement.getStatus());
        assertEquals("plot_1", agreement.getPlotId());
        assertEquals(BigDecimal.valueOf(500), agreement.getRentPrice());
        assertEquals(BigDecimal.valueOf(1000), agreement.getDeposit());

        long days = Duration.between(agreement.getStartDate(), agreement.getEndDate()).toDays();
        assertEquals(3, days);
    }

    @Test
    void testTransferOffer() {
        UUID seller = UUID.randomUUID();
        UUID buyer = UUID.randomUUID();

        TransferOffer offer = TransferOffer.create("plot_2", seller, "Sprzedawca", buyer, "Kupujacy", BigDecimal.valueOf(15000), 300);

        assertEquals(TransferStatus.PENDING, offer.getStatus());
        assertFalse(offer.isExpired());
        assertEquals(BigDecimal.valueOf(15000), offer.getPrice());
    }

    @Test
    void testAuctionBiddingAndSnipe() {
        PlotAuction auction = PlotAuction.create("plot_auc", UUID.randomUUID(), BigDecimal.valueOf(10000), BigDecimal.valueOf(500), 10, BigDecimal.valueOf(25000));

        assertEquals(AuctionStatus.ACTIVE, auction.getStatus());
        assertEquals(BigDecimal.valueOf(10000), auction.getCurrentBid());

        UUID b1 = UUID.randomUUID();
        boolean bid1 = auction.placeBid(b1, "Gracz1", BigDecimal.valueOf(10000), 120);
        assertTrue(bid1);
        assertEquals(b1, auction.getCurrentBidderUuid());

        UUID b2 = UUID.randomUUID();
        boolean bidFail = auction.placeBid(b2, "Gracz2", BigDecimal.valueOf(10200), 120);
        assertFalse(bidFail);

        boolean bid2 = auction.placeBid(b2, "Gracz2", BigDecimal.valueOf(11000), 120);
        assertTrue(bid2);
        assertEquals(BigDecimal.valueOf(11000), auction.getCurrentBid());
        assertEquals(b2, auction.getCurrentBidderUuid());
    }
}
