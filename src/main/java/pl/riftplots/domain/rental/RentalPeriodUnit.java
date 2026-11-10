package pl.riftplots.domain.rental;

import java.time.Duration;

public enum RentalPeriodUnit {
    HOUR, DAY, WEEK, MONTH;

    public Duration getDuration() {
        return Duration.ofSeconds(pl.riftplots.config.Settings.longValue("rental-period-seconds." + name()));
    }

    public static RentalPeriodUnit parse(String input) {
        if (input == null || input.isBlank()) {
            return DAY;
        }
        try {
            return RentalPeriodUnit.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DAY;
        }
    }
}
