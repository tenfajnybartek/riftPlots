package pl.riftplots.util;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class TimeUtil {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    private TimeUtil() {
    }

    public static String formatInstant(Instant instant) {
        if (instant == null) {
            return "Nigdy / Brak";
        }
        return DATE_FORMATTER.format(instant);
    }

    public static String formatRemainingTime(Instant targetTime) {
        if (targetTime == null) {
            return "Brak";
        }
        Instant now = Instant.now();
        if (targetTime.isBefore(now)) {
            return "0s (upłynął)";
        }
        Duration duration = Duration.between(now, targetTime);
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("d ");
        }
        if (hours > 0 || days > 0) {
            sb.append(hours).append("h ");
        }
        if (minutes > 0 || hours > 0 || days > 0) {
            sb.append(minutes).append("m ");
        }
        sb.append(seconds).append("s");
        return sb.toString().trim();
    }
}
