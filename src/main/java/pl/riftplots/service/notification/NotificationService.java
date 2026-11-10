package pl.riftplots.service.notification;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import pl.riftplots.util.AdventureUtil;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationService {
    private final NotificationStore store;

    public NotificationService() {
        this(null, 30, 200);
    }

    public NotificationService(java.io.File folder, int retentionDays, int maxHistory) {
        store = new NotificationStore(folder, retentionDays, maxHistory);
        store.initialize();
    }

    public NotificationStore getStore() {
        return store;
    }

    public void notifyPlayer(UUID playerUuid, String message, Sound sound) {
        notifyPlayer(playerUuid, message, sound != null ? org.bukkit.Registry.SOUND_EVENT.getKey(sound).asString() : (String) null);
    }

    public void notifyPlayer(UUID playerUuid, String message, String sound) {
        if (playerUuid == null || message == null || message.isBlank()) {
            return;
        }

        NotificationStore.Notice notice;
        try {
            notice = store.enqueue(playerUuid, message);
        } catch (RuntimeException e) {
            java.util.logging.Logger.getLogger("riftPlots").log(java.util.logging.Level.SEVERE, "Nie można zapisać powiadomienia", e);
            return;
        }
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(AdventureUtil.parse(message));
            try {
                store.delivered(playerUuid, notice.id());
            } catch (RuntimeException e) {
                java.util.logging.Logger.getLogger("riftPlots").log(java.util.logging.Level.SEVERE, "Nie można potwierdzić wysłania powiadomienia", e);
            }
            if (sound != null) {
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            }
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
        var queued = store.pending(player.getUniqueId());
        if (queued != null && !queued.isEmpty()) {
            player.sendMessage(AdventureUtil.parse("<gradient:#00c6ff:#0072ff><b>=== TWOJE POWIADOMIENIA PODCZAS NIEOBECNOŚCI ===</b></gradient>"));
            for (var notice : queued) {
                player.sendMessage(AdventureUtil.parse(notice.message()));
                store.delivered(player.getUniqueId(), notice.id());
            }
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        }
    }
}
