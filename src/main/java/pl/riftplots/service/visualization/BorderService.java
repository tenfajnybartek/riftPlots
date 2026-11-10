package pl.riftplots.service.visualization;

import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.riftplots.domain.plot.*;

import java.time.Instant;
import java.util.*;

public final class BorderService implements AutoCloseable {
    public record Point(double x, double y, double z) {
    }

    private final Plugin plugin;
    private final Map<UUID, BukkitTask> tasks = new HashMap<>();
    private final Map<UUID, Instant> cooldowns = new HashMap<>();

    public BorderService(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean show(Player player, Plot plot) {
        if (!player.getWorld().getName().equalsIgnoreCase(plot.getBounds().getWorldName())) return false;
        int cooldown = Math.clamp(plugin.getConfig().getInt("visualization.border-cooldown-seconds", 10), 5, 120);
        if (cooldowns.getOrDefault(player.getUniqueId(), Instant.EPOCH).plusSeconds(cooldown).isAfter(Instant.now()))
            return false;
        cancel(player.getUniqueId());
        cooldowns.put(player.getUniqueId(), Instant.now());
        int limit = Math.clamp(plugin.getConfig().getInt("visualization.border-points", 120), 24, 240);
        var points = points(plot.getBounds(), limit);
        int frames = Math.clamp(plugin.getConfig().getInt("visualization.border-duration-seconds", 10), 3, 30) * 2;
        var dust = new Particle.DustOptions(Color.fromRGB(0, 198, 255), 1.1f);
        BukkitTask task = new BukkitRunnable() {
            int remaining = frames;

            public void run() {
                if (remaining-- <= 0 || !player.isOnline() || !player.getWorld().getName().equalsIgnoreCase(plot.getBounds().getWorldName())) {
                    cancel();
                    tasks.remove(player.getUniqueId());
                    return;
                }
                for (Point point : points)
                    player.spawnParticle(Particle.DUST, point.x(), point.y(), point.z(), 1, 0, 0, 0, 0, dust);
            }
        }.runTaskTimer(plugin, 0L, 10L);
        tasks.put(player.getUniqueId(), task);
        return true;
    }

    public void cancel(UUID player) {
        var task = tasks.remove(player);
        if (task != null) task.cancel();
    }

    public void forget(UUID player) {
        cancel(player);
        cooldowns.remove(player);
    }

    public void close() {
        tasks.values().forEach(BukkitTask::cancel);
        tasks.clear();
        cooldowns.clear();
    }

    public static List<Point> points(PlotBounds bounds, int limit) {
        double[] x = {bounds.getMinX(), bounds.getMaxX() + 1.0};
        double[] y = {bounds.getMinY(), bounds.getMaxY() + 1.0};
        double[] z = {bounds.getMinZ(), bounds.getMaxZ() + 1.0};
        List<Point> result = new ArrayList<>();
        int perEdge = Math.max(2, limit / 12);
        for (int axis = 0; axis < 3; axis++)
            for (int a = 0; a < 2; a++)
                for (int b = 0; b < 2; b++) {
                    for (int i = 0; i < perEdge; i++) {
                        double t = (double) i / (perEdge - 1);
                        result.add(switch (axis) {
                            case 0 -> new Point(x[0] + t * (x[1] - x[0]), y[a], z[b]);
                            case 1 -> new Point(x[a], y[0] + t * (y[1] - y[0]), z[b]);
                            default -> new Point(x[a], y[b], z[0] + t * (z[1] - z[0]));
                        });
                    }
                }
        return List.copyOf(result);
    }
}
