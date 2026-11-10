package pl.riftplots.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

import pl.riftplots.config.*;

public final class TimeUtil {
    private TimeUtil() {
    }

    public static String formatInstant(Instant instant) {
        if (instant == null) return Texts.raw("format.time.never");
        String zone = Settings.string("formatting.timezone");
        return DateTimeFormatter.ofPattern(Settings.string("formatting.date-pattern"))
                .withZone(zone.equalsIgnoreCase("system") ? ZoneId.systemDefault() : ZoneId.of(zone)).format(instant);
    }

    public static String formatRemainingTime(Instant target) {
        if (target == null) return Texts.raw("format.time.none");
        Instant now = Instant.now();
        if (!target.isAfter(now)) return Texts.raw("format.time.expired");
        Duration duration = Duration.between(now, target);
        var result = new StringBuilder();
        if (duration.toDays() > 0) result.append(Texts.raw("format.time.days", duration.toDays()));
        if (duration.toHoursPart() > 0 || duration.toDays() > 0)
            result.append(Texts.raw("format.time.hours", duration.toHoursPart()));
        if (duration.toMinutesPart() > 0 || duration.toHoursPart() > 0 || duration.toDays() > 0)
            result.append(Texts.raw("format.time.minutes", duration.toMinutesPart()));
        result.append(Texts.raw("format.time.seconds", duration.toSecondsPart()));
        return result.toString().trim();
    }
}
