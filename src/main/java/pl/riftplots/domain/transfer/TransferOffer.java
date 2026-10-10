package pl.riftplots.domain.transfer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TransferOffer {
    private final String id;
    private final String plotId;
    private final UUID sellerUuid;
    private final String sellerName;
    private final UUID buyerUuid;
    private final String buyerName;
    private final BigDecimal price;
    private final Instant createdAt;
    private final Instant expiresAt;
    private TransferStatus status;

    public TransferOffer(String id,
                         String plotId,
                         UUID sellerUuid,
                         String sellerName,
                         UUID buyerUuid,
                         String buyerName,
                         BigDecimal price,
                         Instant createdAt,
                         Instant expiresAt,
                         TransferStatus status) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.plotId = Objects.requireNonNull(plotId, "plotId cannot be null").toLowerCase();
        this.sellerUuid = Objects.requireNonNull(sellerUuid, "sellerUuid cannot be null");
        this.sellerName = sellerName != null ? sellerName : "Unknown";
        this.buyerUuid = Objects.requireNonNull(buyerUuid, "buyerUuid cannot be null");
        this.buyerName = buyerName != null ? buyerName : "Unknown";
        this.price = price != null ? price.max(BigDecimal.ZERO) : BigDecimal.ZERO;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.expiresAt = expiresAt != null ? expiresAt : Instant.now().plusSeconds(300);
        this.status = status != null ? status : TransferStatus.PENDING;
    }

    public static TransferOffer create(String plotId, UUID sellerUuid, String sellerName, UUID buyerUuid, String buyerName, BigDecimal price, int validSeconds) {
        String randomId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(validSeconds > 0 ? validSeconds : 300);
        return new TransferOffer(randomId, plotId, sellerUuid, sellerName, buyerUuid, buyerName, price, now, exp, TransferStatus.PENDING);
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

    public String getSellerName() {
        return sellerName;
    }

    public UUID getBuyerUuid() {
        return buyerUuid;
    }

    public String getBuyerName() {
        return buyerName;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public TransferStatus getStatus() {
        return status;
    }

    public void setStatus(TransferStatus status) {
        this.status = status;
    }

    public boolean isExpired() {
        return status == TransferStatus.PENDING && Instant.now().isAfter(expiresAt);
    }
}
