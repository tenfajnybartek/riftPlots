package pl.riftplots.domain.rental;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class RentalAgreement {
    private final String id;
    private final String plotId;
    private final UUID ownerUuid;
    private final UUID tenantUuid;
    private final String tenantName;
    private final BigDecimal rentPrice;
    private final RentalPeriodUnit periodUnit;
    private final BigDecimal deposit;
    private final Instant startDate;
    private Instant endDate;
    private boolean autoRenew;
    private RentalStatus status;

    public RentalAgreement(String id,
                           String plotId,
                           UUID ownerUuid,
                           UUID tenantUuid,
                           String tenantName,
                           BigDecimal rentPrice,
                           RentalPeriodUnit periodUnit,
                           BigDecimal deposit,
                           Instant startDate,
                           Instant endDate,
                           boolean autoRenew,
                           RentalStatus status) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.plotId = Objects.requireNonNull(plotId, "plotId cannot be null").toLowerCase();
        this.ownerUuid = ownerUuid;
        this.tenantUuid = Objects.requireNonNull(tenantUuid, "tenantUuid cannot be null");
        this.tenantName = tenantName != null ? tenantName : "Unknown";
        this.rentPrice = rentPrice != null ? rentPrice : BigDecimal.ZERO;
        this.periodUnit = periodUnit != null ? periodUnit : RentalPeriodUnit.DAY;
        this.deposit = deposit != null ? deposit : BigDecimal.ZERO;
        this.startDate = startDate != null ? startDate : Instant.now();
        this.endDate = endDate != null ? endDate : startDate.plus(this.periodUnit.getDuration());
        this.autoRenew = autoRenew;
        this.status = status != null ? status : RentalStatus.ACTIVE;
    }

    public static RentalAgreement create(String plotId, UUID ownerUuid, UUID tenantUuid, String tenantName, BigDecimal rentPrice, RentalPeriodUnit periodUnit, BigDecimal deposit, int periodsCount, boolean autoRenew) {
        String randomId = UUID.randomUUID().toString();
        Instant start = Instant.now();
        long totalSeconds = periodUnit.getDuration().toSeconds() * Math.max(1, periodsCount);
        Instant end = start.plusSeconds(totalSeconds);
        return new RentalAgreement(randomId, plotId, ownerUuid, tenantUuid, tenantName, rentPrice, periodUnit, deposit, start, end, autoRenew, RentalStatus.ACTIVE);
    }

    public String getId() {
        return id;
    }

    public String getPlotId() {
        return plotId;
    }

    public UUID getOwnerUuid() {
        return ownerUuid;
    }

    public UUID getTenantUuid() {
        return tenantUuid;
    }

    public String getTenantName() {
        return tenantName;
    }

    public BigDecimal getRentPrice() {
        return rentPrice;
    }

    public RentalPeriodUnit getPeriodUnit() {
        return periodUnit;
    }

    public BigDecimal getDeposit() {
        return deposit;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public void setEndDate(Instant endDate) {
        this.endDate = endDate;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public RentalStatus getStatus() {
        return status;
    }

    public void setStatus(RentalStatus status) {
        this.status = status;
    }

    public boolean isActive() {
        return status == RentalStatus.ACTIVE && Instant.now().isBefore(endDate);
    }
}
