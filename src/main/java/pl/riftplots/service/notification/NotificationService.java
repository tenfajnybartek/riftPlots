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
        this(null, pl.riftplots.config.Settings.integer("notifications.retention-days"), pl.riftplots.config.Settings.integer("notifications.max-history-per-player"));
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
            java.util.logging.Logger.getLogger("riftPlots").log(java.util.logging.Level.SEVERE, pl.riftplots.config.Texts.raw("text.notification-service.nie-mozna-zapisac-powiadomienia"), e);
            return;
        }
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(AdventureUtil.parse(message));
            try {
                store.delivered(playerUuid, notice.id());
            } catch (RuntimeException e) {
                java.util.logging.Logger.getLogger("riftPlots").log(java.util.logging.Level.SEVERE, pl.riftplots.config.Texts.raw("text.notification-service.nie-mozna-potwierdzic-wysania-powiadomienia"), e);
            }
            if (sound != null && !sound.isBlank()) {
                player.playSound(player.getLocation(), sound, (float) pl.riftplots.config.Settings.decimal("notifications.sound-volume"), (float) pl.riftplots.config.Settings.decimal("notifications.sound-pitch"));
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
        AdventureUtil.sendTitle(player, title, subtitle, Duration.ofMillis(pl.riftplots.config.Settings.longValue("notifications.title-fade-in-ms")), Duration.ofMillis(pl.riftplots.config.Settings.longValue("notifications.title-stay-ms")), Duration.ofMillis(pl.riftplots.config.Settings.longValue("notifications.title-fade-out-ms")));
        if (sound != null) {
            player.playSound(player.getLocation(), sound, (float) pl.riftplots.config.Settings.decimal("notifications.sound-volume"), (float) pl.riftplots.config.Settings.decimal("notifications.sound-pitch"));
        }
    }

    public void deliverOfflineMessages(Player player) {
        if (player == null) {
            return;
        }
        var queued = store.pending(player.getUniqueId());
        if (queued != null && !queued.isEmpty()) {
            player.sendMessage(AdventureUtil.parse(pl.riftplots.config.Texts.markup("text.notification-service.twoje-powiadomienia-podczas-nieobecnosci")));
            for (var notice : queued) {
                player.sendMessage(AdventureUtil.parse(notice.message()));
                store.delivered(player.getUniqueId(), notice.id());
            }
            String sound = pl.riftplots.config.Settings.string("sounds.offline-messages");
            if (!sound.isBlank())
                player.playSound(player.getLocation(), sound, (float) pl.riftplots.config.Settings.decimal("notifications.sound-volume"), (float) pl.riftplots.config.Settings.decimal("notifications.sound-pitch"));
        }
    }
}
