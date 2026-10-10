package pl.riftplots.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;

public final class AdventureUtil {
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
    private static final LegacyComponentSerializer AMPERSAND_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();

    private AdventureUtil() {
    }

    public static Component parse(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        if (input.contains("&") || input.contains("§")) {
            return AMPERSAND_SERIALIZER.deserialize(input.replace('§', '&'));
        }
        return MINI_MESSAGE.deserialize(input);
    }

    public static Component parse(String input, Map<String, String> placeholders) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        String replaced = input;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                replaced = replaced.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return parse(replaced);
    }

    public static void sendMessage(CommandSender sender, String message) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }
        sender.sendMessage(parse(message));
    }

    public static void sendMessage(CommandSender sender, String message, Map<String, String> placeholders) {
        if (sender == null || message == null || message.isBlank()) {
            return;
        }
        sender.sendMessage(parse(message, placeholders));
    }

    public static void sendActionBar(Player player, String message) {
        if (player == null || message == null || message.isBlank()) {
            return;
        }
        player.sendActionBar(parse(message));
    }

    public static void sendTitle(Player player, String titleText, String subtitleText, Duration fadeIn, Duration stay, Duration fadeOut) {
        if (player == null) {
            return;
        }
        Component titleComp = parse(titleText != null ? titleText : "");
        Component subComp = parse(subtitleText != null ? subtitleText : "");
        Title.Times times = Title.Times.times(fadeIn, stay, fadeOut);
        Title title = Title.title(titleComp, subComp, times);
        player.showTitle(title);
    }
}
