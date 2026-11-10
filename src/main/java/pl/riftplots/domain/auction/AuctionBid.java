package pl.riftplots.domain.auction;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class AuctionBid {
    private final UUID bidderUuid;
    private final String bidderName;
    private final BigDecimal amount;
    private final Instant timestamp;

    public AuctionBid(UUID bidderUuid, String bidderName, BigDecimal amount, Instant timestamp) {
        this.bidderUuid = Objects.requireNonNull(bidderUuid, pl.riftplots.config.Texts.raw("text.auction-bid.bidderuuid-cannot-be-null"));
        this.bidderName = bidderName != null ? bidderName : pl.riftplots.config.Texts.raw("text.auction-bid.unknown");
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public UUID getBidderUuid() {
        return bidderUuid;
    }

    public String getBidderName() {
        return bidderName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
