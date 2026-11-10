package pl.riftplots.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.*;

/**
 * Shared catalog also available to domain validation and background diagnostics.
 */
public final class Texts {
    private static volatile MessageConfiguration active = new MessageConfiguration(null);

    private Texts() {
    }

    public static void install(MessageConfiguration messages) {
        active = Objects.requireNonNull(messages);
    }

    public static MessageConfiguration catalog() {
        return active;
    }

    public static String raw(String key, Object... arguments) {
        return active.format(key, false, arguments);
    }

    public static String markup(String key, Object... arguments) {
        return active.format(key, true, arguments);
    }

    public static Component component(String key, Object... arguments) {
        return MiniMessage.miniMessage().deserialize(markup(key, arguments));
    }

    public static String label(Object value) {
        if (value == null) return raw("labels.none");
        if (value instanceof java.time.Instant instant) return pl.riftplots.util.TimeUtil.formatInstant(instant);
        if (value instanceof Boolean bool) return raw(bool ? "labels.enabled" : "labels.disabled");
        if (value instanceof Enum<?> enumeration) {
            String key = "labels." + enumeration.getDeclaringClass().getSimpleName() + "." + enumeration.name();
            return active.contains(key) ? raw(key) : enumeration.name();
        }
        return value.toString();
    }

    public static Map<String, String> variables(Object... pairs) {
        if (pairs.length % 2 != 0) throw new IllegalArgumentException("pairs");
        Map<String, String> result = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2)
            result.put(pairs[index].toString(), label(pairs[index + 1]));
        return result;
    }
}
