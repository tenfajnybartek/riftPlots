package pl.riftplots.service.teleport;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.integration.vault.VaultHook;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeleportService {
    private final PlotManager plotManager;
    private final VaultHook vaultHook;
    private final pl.riftplots.config.PluginConfiguration config;
    private final pl.riftplots.config.MessageConfiguration messageConfig;
    private final Map<UUID, Instant> cooldowns = new ConcurrentHashMap<>();

    public TeleportService(PlotManager plotManager,
                           VaultHook vaultHook,
                           pl.riftplots.config.PluginConfiguration config,
                           pl.riftplots.config.MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.vaultHook = vaultHook;
        this.config = config;
        this.messageConfig = messageConfig;
    }

    public boolean setHome(Player player, Plot plot) {
        if (player == null || plot == null) return false;
        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("riftplots.admin.bypass")) {
            player.sendMessage(messageConfig.get("plot.not-owner", Map.of("plot_id", plot.getId())));
            return false;
        }

        Location loc = player.getLocation();
        if (!plot.getBounds().contains(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
            player.sendMessage(messageConfig.get("teleport.outside-bounds"));
            return false;
        }

        plot.setHomeLocation(loc.getWorld().getName(), loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        plotManager.savePlot(plot);
        player.sendMessage(messageConfig.get("teleport.set-success", Map.of("name", plot.getName())));
        return true;
    }

    public boolean teleportHome(Player player, Plot plot) {
        if (player == null || plot == null) return false;

        if (!config.isTeleportEnabled() && !player.hasPermission("riftplots.admin.bypass")) {
            player.sendMessage(messageConfig.get("teleport.disabled"));
            return false;
        }

        if (plot.getStatus() == PlotStatus.RESETTING || plot.getStatus() == PlotStatus.DISABLED) {
            player.sendMessage(messageConfig.get("plot.disabled", Map.of("plot_id", plot.getId())));
            return false;
        }

        if (!plot.isOwner(player.getUniqueId()) && !(plotManager.hasUsableMembership(plot, player.getUniqueId())
                && plot.hasPermission(player.getUniqueId(), pl.riftplots.domain.plot.PlotPermission.TELEPORT)) && !player.hasPermission("riftplots.admin.bypass")) {
            player.sendMessage(messageConfig.get("plot.not-member"));
            return false;
        }

        UUID uuid = player.getUniqueId();
        if (!player.hasPermission("riftplots.admin.bypass")) {
            Instant last = cooldowns.get(uuid);
            if (last != null) {
                long elapsed = java.time.Duration.between(last, Instant.now()).getSeconds();
                int cd = config.getTeleportCooldownSeconds();
                if (elapsed < cd) {
                    player.sendMessage(messageConfig.get("teleport.cooldown", Map.of("seconds", String.valueOf(cd - elapsed))));
                    return false;
                }
            }
        }

        Location targetLoc = resolveSafeLocation(plot);
        if (targetLoc == null) {
            player.sendMessage(messageConfig.get("teleport.unsafe"));
            return false;
        }

        BigDecimal cost = config.getTeleportCost();
        if (cost.compareTo(BigDecimal.ZERO) > 0 && !player.hasPermission("riftplots.admin.bypass")) {
            if (!vaultHook.has(player, cost)) {
                player.sendMessage(messageConfig.get("teleport.cannot-afford", Map.of("cost", config.formatMoney(cost))));
                return false;
            }
            if (!vaultHook.withdraw(player, cost)) {
                player.sendMessage(messageConfig.get("general.economy-error"));
                return false;
            }
        }

        if (!player.teleport(targetLoc)) {
            if (cost.signum() > 0 && !player.hasPermission("riftplots.admin.bypass")) {
                vaultHook.deposit(player, cost);
            }
            return false;
        }
        cooldowns.put(uuid, Instant.now());
        player.sendMessage(messageConfig.get("teleport.success", Map.of("name", plot.getName())));
        return true;
    }

    private Location resolveSafeLocation(Plot plot) {
        World world = Bukkit.getWorld(plot.getBounds().getWorldName());
        if (world == null) return null;

        if (plot.hasHomeLocation() && world.getName().equals(plot.getHomeWorld())) {
            Location home = new Location(world, plot.getHomeX(), plot.getHomeY(), plot.getHomeZ(),
                    plot.getHomeYaw() != null ? plot.getHomeYaw() : 0f,
                    plot.getHomePitch() != null ? plot.getHomePitch() : 0f);
            if (isInside(plot, home) && isSafe(home)) {
                return home;
            }
        }

        int centerX = (plot.getBounds().getMinX() + plot.getBounds().getMaxX()) / 2;
        int centerZ = (plot.getBounds().getMinZ() + plot.getBounds().getMaxZ()) / 2;
        int maxY = Math.min(plot.getBounds().getMaxY() - 1, world.getMaxHeight() - 2);
        int minY = Math.max(plot.getBounds().getMinY(), world.getMinHeight() + 1);
        for (int y = maxY; y >= minY; y--) {
            Location candidate = new Location(world, centerX + 0.5, y, centerZ + 0.5);
            if (isSafe(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean isInside(Plot plot, Location location) {
        return plot.getBounds().contains(location.getWorld().getName(), location.getBlockX(), location.getBlockY(), location.getBlockZ())
                && location.getBlockY() + 1 <= plot.getBounds().getMaxY();
    }

    private boolean isSafe(Location loc) {
        Block feet = loc.getBlock();
        Block head = feet.getRelative(0, 1, 0);
        Block below = feet.getRelative(0, -1, 0);

        if (!below.isSolid()) return false;
        if (isHazard(below.getType())) return false;
        return feet.isPassable() && head.isPassable() && !feet.isLiquid() && !head.isLiquid()
                && !isHazard(feet.getType()) && !isHazard(head.getType());
    }

    private boolean isHazard(Material material) {
        return switch (material) {
            case LAVA, FIRE, SOUL_FIRE, MAGMA_BLOCK, CACTUS, CAMPFIRE, SOUL_CAMPFIRE,
                 POWDER_SNOW, SWEET_BERRY_BUSH, WITHER_ROSE, END_PORTAL, NETHER_PORTAL -> true;
            default -> false;
        };
    }
}
