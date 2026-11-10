package pl.riftplots.service.notification;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.storage.yaml.YamlDataFile;

import java.io.File;
import java.io.IOException;
import java.time.*;
import java.util.*;

public final class NotificationStore {
    public record Notice(String id, UUID player, String message, Instant created, Instant delivered) {
    }

    private final File directory;
    private final int retentionDays;
    private final int maxHistory;
    private final Map<UUID, List<Notice>> records = new HashMap<>();

    public NotificationStore(File dataFolder, int retentionDays, int maxHistory) {
        directory = dataFolder != null ? new File(dataFolder, "data/notifications") : null;
        this.retentionDays = Math.max(1, retentionDays);
        this.maxHistory = Math.max(1, maxHistory);
    }

    public synchronized void initialize() {
        if (directory == null) return;
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) {
            if (directory.exists() && !directory.isDirectory())
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.notification-store.invalid-notifications-directory"));
            return;
        }
        for (File file : files) {
            UUID player = UUID.fromString(file.getName().replace(".yml", ""));
            var yaml = YamlDataFile.load(file);
            if (yaml.getInt("format", 1) != 1)
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.notification-store.unsupported-notification-format", file));
            List<Notice> notices = new ArrayList<>();
            for (var row : yaml.getMapList("messages")) {
                long delivered = Long.parseLong(row.get("delivered").toString());
                notices.add(new Notice(row.get("id").toString(), player, row.get("message").toString(),
                        Instant.ofEpochMilli(Long.parseLong(row.get("created").toString())), delivered > 0 ? Instant.ofEpochMilli(delivered) : null));
            }
            records.put(player, notices);
        }
    }

    public synchronized Notice enqueue(UUID player, String message) {
        Notice notice = new Notice(UUID.randomUUID().toString(), player, message, Instant.now(), null);
        List<Notice> changed = new ArrayList<>(records.getOrDefault(player, List.of()));
        changed.add(notice);
        save(player, changed);
        return notice;
    }

    public synchronized List<Notice> pending(UUID player) {
        return records.getOrDefault(player, List.of()).stream().filter(n -> n.delivered() == null).toList();
    }

    public synchronized List<Notice> history(UUID player, int page) {
        var entries = new ArrayList<>(records.getOrDefault(player, List.of()));
        Collections.reverse(entries);
        return entries.stream().skip((long) Math.max(0, page) * pl.riftplots.config.Settings.integer("paging.notifications")).limit(pl.riftplots.config.Settings.integer("paging.notifications")).toList();
    }

    public synchronized void delivered(UUID player, String id) {
        var changed = records.getOrDefault(player, List.of()).stream().map(n -> n.id().equals(id)
                ? new Notice(n.id(), player, n.message(), n.created(), Instant.now()) : n).toList();
        save(player, changed);
    }

    private void save(UUID player, List<Notice> notices) {
        Instant threshold = Instant.now().minus(Duration.ofDays(retentionDays));
        List<Notice> kept = new ArrayList<>();
        int deliveredCount = 0;
        for (int i = notices.size() - 1; i >= 0; i--) {
            Notice notice = notices.get(i);
            // Undelivered messages are debts of the notification system and are never silently truncated.
            if (notice.delivered() == null || notice.created().isAfter(threshold) && deliveredCount++ < maxHistory)
                kept.add(notice);
        }
        Collections.reverse(kept);
        if (directory != null) {
            var yaml = new YamlConfiguration();
            yaml.set("format", 1);
            yaml.set("messages", kept.stream().map(n -> Map.of("id", n.id(), "message", n.message(),
                    "created", n.created().toEpochMilli(), "delivered", n.delivered() != null ? n.delivered().toEpochMilli() : 0)).toList());
            try {
                YamlDataFile.save(yaml, new File(directory, player + ".yml"));
            } catch (IOException e) {
                throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.notification-store.cannot-persist-notification-for", player), e);
            }
        }
        records.put(player, kept);
    }
}
