package pl.riftplots.domain.rental;

import java.time.Duration;

public enum RentalPeriodUnit {
    HOUR(Duration.ofHours(1)),
    DAY(Duration.ofDays(1)),
    WEEK(Duration.ofDays(7)),
    MONTH(Duration.ofDays(30));

    private final Duration duration;

    RentalPeriodUnit(Duration duration) {
        this.duration = duration;
    }

    public Duration getDuration() {
        return duration;
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
