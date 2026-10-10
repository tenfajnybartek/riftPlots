package pl.riftplots.domain.plot;

public enum PlotStatus {
    AVAILABLE,
    OWNED,
    PAYMENT_DUE,
    GRACE_PERIOD,
    RESETTING,
    DISABLED;

    public boolean canBePurchased() {
        return this == AVAILABLE;
    }

    public boolean isOwned() {
        return this == OWNED || this == PAYMENT_DUE || this == GRACE_PERIOD;
    }

    public boolean isInGracePeriod() {
        return this == GRACE_PERIOD;
    }

    public boolean isResetting() {
        return this == RESETTING;
    }

    public boolean isDisabled() {
        return this == DISABLED;
    }
}
