package pl.riftplots.domain.payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class TransactionRecord {
    private final String id;
    private final String plotId;
    private final UUID playerUuid;
    private final String playerName;
    private final TransactionType type;
    private final BigDecimal amount;
    private final TransactionStatus status;
    private final Instant timestamp;
    private final String details;

    public TransactionRecord(String id,
                             String plotId,
                             UUID playerUuid,
                             String playerName,
                             TransactionType type,
                             BigDecimal amount,
                             TransactionStatus status,
                             Instant timestamp,
                             String details) {
        this.id = Objects.requireNonNull(id, pl.riftplots.config.Texts.raw("text.transaction-record.id-cannot-be-null"));
        this.plotId = Objects.requireNonNull(plotId, pl.riftplots.config.Texts.raw("text.transaction-record.plotid-cannot-be-null"));
        this.playerUuid = Objects.requireNonNull(playerUuid, pl.riftplots.config.Texts.raw("text.transaction-record.playeruuid-cannot-be-null"));
        this.playerName = playerName != null ? playerName : pl.riftplots.config.Texts.raw("text.transaction-record.unknown");
        this.type = type != null ? type : TransactionType.TAX;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.status = status != null ? status : TransactionStatus.SUCCESS;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.details = details != null ? details : "";
    }

    public static TransactionRecord create(String plotId, UUID playerUuid, String playerName, TransactionType type, BigDecimal amount, TransactionStatus status, String details) {
        String randomId = UUID.randomUUID().toString();
        return new TransactionRecord(randomId, plotId, playerUuid, playerName, type, amount, status, Instant.now(), details);
    }

    public String getId() {
        return id;
    }

    public String getPlotId() {
        return plotId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getPlayerName() {
        return playerName;
    }

    public TransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDetails() {
        return details;
    }
}
