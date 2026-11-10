package pl.riftplots.domain.city;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class CityTransaction {
    private final String id;
    private final CityTransactionType type;
    private final BigDecimal amount;
    private final BigDecimal balanceAfter;
    private final Instant timestamp;
    private final String plotId;
    private final UUID playerUuid;
    private final String description;

    public CityTransaction(String id,
                           CityTransactionType type,
                           BigDecimal amount,
                           BigDecimal balanceAfter,
                           Instant timestamp,
                           String plotId,
                           UUID playerUuid,
                           String description) {
        this.id = Objects.requireNonNull(id, pl.riftplots.config.Texts.raw("text.city-transaction.id-cannot-be-null"));
        this.type = type != null ? type : CityTransactionType.TAX_INCOME;
        this.amount = amount != null ? amount : BigDecimal.ZERO;
        this.balanceAfter = balanceAfter != null ? balanceAfter : BigDecimal.ZERO;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
        this.plotId = plotId;
        this.playerUuid = playerUuid;
        this.description = description != null ? description : "";
    }

    public static CityTransaction create(CityTransactionType type, BigDecimal amount, BigDecimal balanceAfter, String plotId, UUID playerUuid, String description) {
        return new CityTransaction(UUID.randomUUID().toString(), type, amount, balanceAfter, Instant.now(), plotId, playerUuid, description);
    }

    public String getId() {
        return id;
    }

    public CityTransactionType getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getPlotId() {
        return plotId;
    }

    public UUID getPlayerUuid() {
        return playerUuid;
    }

    public String getDescription() {
        return description;
    }
}
