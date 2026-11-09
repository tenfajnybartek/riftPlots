package pl.riftplots.service.notification;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pl.riftplots.util.AdventureUtil;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationService {
    private final Map<UUID, List<String>> offlineQueue = new ConcurrentHashMap<>();

    public void notifyPlayer(UUID playerUuid, String message, Sound sound) {
        notifyPlayer(playerUuid, message, sound != null ? org.bukkit.Registry.SOUND_EVENT.getKey(sound).asString() : (String) null);
    }

    public void notifyPlayer(UUID playerUuid, String message, String sound) {
        if (playerUuid == null || message == null || message.isBlank()) {
            return;
        }

        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(AdventureUtil.parse(message));
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
        } else {
            offlineQueue.computeIfAbsent(playerUuid, k -> Collections.synchronizedList(new ArrayList<>())).add(message);
        }
    }

    public void notifyActionBar(Player player, String message) {
        if (player != null && message != null) {
            AdventureUtil.sendActionBar(player, message);
        }
    }

    public void notifyTitle(Player player, String title, String subtitle, Sound sound) {
        if (player == null) {
            return;
        }
        AdventureUtil.sendTitle(player, title, subtitle, Duration.ofMillis(300), Duration.ofSeconds(3), Duration.ofMillis(500));
        if (sound != null) {
            player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
        }
    }

    public void deliverOfflineMessages(Player player) {
        if (player == null) {
            return;
        }
        List<String> queued = offlineQueue.remove(player.getUniqueId());
        if (queued != null && !queued.isEmpty()) {
            player.sendMessage(AdventureUtil.parse("<gradient:#00c6ff:#0072ff><b>=== TWOJE POWIADOMIENIA PODCZAS NIEOBECNOŚCI ===</b></gradient>"));
            for (String msg : queued) {
                player.sendMessage(AdventureUtil.parse(msg));
            }
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}
