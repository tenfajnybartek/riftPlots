package pl.riftplots.domain.auction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

public class PlotAuction {
    private final String id;
    private final String plotId;
    private final UUID sellerUuid;
    private final BigDecimal startingPrice;
    private final BigDecimal minBidIncrement;
    private BigDecimal currentBid;
    private UUID currentBidderUuid;
    private String currentBidderName;
    private final Instant startTime;
    private Instant endTime;
    private final BigDecimal buyoutPrice;
    private final List<AuctionBid> bids = new ArrayList<>();
    private AuctionStatus status;

    public PlotAuction(String id,
                       String plotId,
                       UUID sellerUuid,
                       BigDecimal startingPrice,
                       BigDecimal minBidIncrement,
                       BigDecimal currentBid,
                       UUID currentBidderUuid,
                       String currentBidderName,
                       Instant startTime,
                       Instant endTime,
                       BigDecimal buyoutPrice,
                       List<AuctionBid> bids,
                       AuctionStatus status) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.plotId = Objects.requireNonNull(plotId, "plotId cannot be null").toLowerCase();
        this.sellerUuid = sellerUuid;
        this.startingPrice = startingPrice != null ? startingPrice : BigDecimal.ZERO;
        this.minBidIncrement = minBidIncrement != null ? minBidIncrement : BigDecimal.valueOf(100);
        this.currentBid = currentBid != null ? currentBid : this.startingPrice;
        this.currentBidderUuid = currentBidderUuid;
        this.currentBidderName = currentBidderName;
        this.startTime = startTime != null ? startTime : Instant.now();
        this.endTime = endTime != null ? endTime : this.startTime.plusSeconds(3600);
        this.buyoutPrice = buyoutPrice;
        if (bids != null) {
            this.bids.addAll(bids);
        }
        this.status = status != null ? status : AuctionStatus.ACTIVE;
    }

    public static PlotAuction create(String plotId, UUID sellerUuid, BigDecimal startingPrice, BigDecimal minBidIncrement, int durationMinutes, BigDecimal buyoutPrice) {
        String randomId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant end = now.plusSeconds((long) Math.max(1, durationMinutes) * 60L);
        return new PlotAuction(randomId, plotId, sellerUuid, startingPrice, minBidIncrement, startingPrice, null, null, now, end, buyoutPrice, Collections.emptyList(), AuctionStatus.ACTIVE);
    }

    public String getId() {
        return id;
    }

    public String getPlotId() {
        return plotId;
    }

    public UUID getSellerUuid() {
        return sellerUuid;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public BigDecimal getMinBidIncrement() {
        return minBidIncrement;
    }

    public BigDecimal getCurrentBid() {
        return currentBid;
    }

    public UUID getCurrentBidderUuid() {
        return currentBidderUuid;
    }

    public String getCurrentBidderName() {
        return currentBidderName;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getBuyoutPrice() {
        return buyoutPrice;
    }

    public List<AuctionBid> getBids() {
        return Collections.unmodifiableList(bids);
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public boolean placeBid(UUID bidderUuid, String bidderName, BigDecimal amount, int antiSnipeExtensionSeconds) {
        Instant now = Instant.now();
        if (status != AuctionStatus.ACTIVE || !now.isBefore(endTime)
                || bidderUuid == null || bidderUuid.equals(sellerUuid) || amount == null || amount.signum() < 0) {
            return false;
        }

        BigDecimal minRequired = currentBidderUuid == null ? startingPrice : currentBid.add(minBidIncrement);
        if (amount.compareTo(minRequired) < 0) {
            return false;
        }

        this.currentBid = amount;
        this.currentBidderUuid = bidderUuid;
        this.currentBidderName = bidderName;
        this.bids.add(new AuctionBid(bidderUuid, bidderName, amount, Instant.now()));

        if (buyoutPrice != null && amount.compareTo(buyoutPrice) >= 0) {
            this.endTime = now;
            return true;
        }
        if (antiSnipeExtensionSeconds > 0) {
            long remainingSeconds = java.time.Duration.between(Instant.now(), endTime).getSeconds();
            if (remainingSeconds < antiSnipeExtensionSeconds) {
                this.endTime = Instant.now().plusSeconds(antiSnipeExtensionSeconds);
            }
        }

        return true;
    }

    public boolean isExpired() {
        return status == AuctionStatus.ACTIVE && !Instant.now().isBefore(endTime);
    }
}
