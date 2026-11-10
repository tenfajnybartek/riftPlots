package pl.riftplots.integration.worldguard;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotBounds;
import pl.riftplots.domain.plot.PlotMember;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public class WorldGuardHook {
    private final Plugin plugin;

    public WorldGuardHook(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("WorldGuard") != null;
    }

    public RegionManager getRegionManager(String worldName) {
        if (!isAvailable()) {
            return null;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            return null;
        }
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        return container.get(BukkitAdapter.adapt(world));
    }

    public boolean createOrUpdateRegion(Plot plot) {
        if (!isAvailable() || plot == null) {
            return false;
        }
        RegionManager manager = getRegionManager(plot.getBounds().getWorldName());
        if (manager == null) {
            return false;
        }

        String regionId = plot.getWorldGuardRegionId();
        PlotBounds bounds = plot.getBounds();
        BlockVector3 min = BlockVector3.at(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
        BlockVector3 max = BlockVector3.at(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());

        ProtectedCuboidRegion region = new ProtectedCuboidRegion(regionId, min, max);
        region.setPriority(10);

        DefaultDomain owners = region.getOwners();
        if (plot.getOwnerUuid() != null) {
            owners.addPlayer(plot.getOwnerUuid());
        }

        DefaultDomain members = region.getMembers();
        for (PlotMember member : plot.getMembers().values()) {
            members.addPlayer(member.getPlayerUuid());
        }

        region.setFlag(Flags.PASSTHROUGH, StateFlag.State.DENY);

        manager.addRegion(region);
        return true;
    }

    public boolean removeRegion(String worldName, String regionId) {
        if (!isAvailable() || worldName == null || regionId == null) {
            return false;
        }
        RegionManager manager = getRegionManager(worldName);
        if (manager == null) {
            return false;
        }
        return manager.removeRegion(regionId) != null;
    }

    public boolean syncMembers(Plot plot) {
        if (!isAvailable() || plot == null) {
            return false;
        }
        RegionManager manager = getRegionManager(plot.getBounds().getWorldName());
        if (manager == null) {
            return false;
        }
        ProtectedRegion region = manager.getRegion(plot.getWorldGuardRegionId());
        if (region == null) {
            return createOrUpdateRegion(plot);
        }

        DefaultDomain owners = new DefaultDomain();
        if (plot.getOwnerUuid() != null) {
            owners.addPlayer(plot.getOwnerUuid());
        }
        region.setOwners(owners);

        DefaultDomain members = new DefaultDomain();
        for (PlotMember member : plot.getMembers().values()) {
            members.addPlayer(member.getPlayerUuid());
        }
        region.setMembers(members);

        return true;
    }

    public ProtectedRegion getRegion(String worldName, String regionId) {
        if (!isAvailable() || worldName == null || regionId == null) {
            return null;
        }
        RegionManager manager = getRegionManager(worldName);
        if (manager == null) {
            return null;
        }
        return manager.getRegion(regionId);
    }

    public void applyTypeFlags(Plot plot, java.util.Set<String> managedFlags, Map<String, String> values) {
        validateTypeFlags(values);
        ProtectedRegion region = getRegion(plot.getBounds().getWorldName(), plot.getWorldGuardRegionId());
        if (region == null) throw new IllegalStateException("Brak regionu działki");
        for (String name : managedFlags) {
            var flag = WorldGuard.getInstance().getFlagRegistry().get(name);
            if (!(flag instanceof StateFlag state)) {
                if (values.containsKey(name))
                    plugin.getLogger().warning("Flaga typu wymaga ręcznej konfiguracji: " + name);
                continue;
            }
            String value = values.get(name);
            if (value == null) region.setFlag(state, null);
            else if (value.equalsIgnoreCase("allow") || value.equalsIgnoreCase("deny")) region.setFlag(state,
                    value.equalsIgnoreCase("allow") ? StateFlag.State.ALLOW : StateFlag.State.DENY);
            else throw new IllegalArgumentException("Flaga " + name + " wymaga allow albo deny.");
        }
    }

    public void validateTypeFlags(Map<String, String> values) {
        for (var entry : values.entrySet()) {
            if (!(WorldGuard.getInstance().getFlagRegistry().get(entry.getKey()) instanceof StateFlag)
                    || !(entry.getValue().equalsIgnoreCase("allow") || entry.getValue().equalsIgnoreCase("deny"))) {
                throw new IllegalArgumentException("Domyślne flagi typu obsługują zarejestrowane flagi allow/deny: " + entry.getKey());
            }
        }
    }

    public PlotBounds getRegionBounds(String worldName, String regionId) {
        ProtectedRegion region = getRegion(worldName, regionId);
        if (region == null) {
            return null;
        }
        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        return new PlotBounds(worldName, min.x(), min.y(), min.z(), max.x(), max.y(), max.z());
    }

    public String findConflictingRegion(PlotBounds bounds, String excludedRegionId) {
        if (!isAvailable() || bounds == null) {
            return null;
        }
        RegionManager manager = getRegionManager(bounds.getWorldName());
        if (manager == null) {
            return null;
        }

        BlockVector3 min = BlockVector3.at(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
        BlockVector3 max = BlockVector3.at(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
        ProtectedCuboidRegion testRegion = new ProtectedCuboidRegion("test_conflict", min, max);

        for (Map.Entry<String, ProtectedRegion> entry : manager.getRegions().entrySet()) {
            String id = entry.getKey();
            if (id.equalsIgnoreCase(excludedRegionId) || id.equalsIgnoreCase("__global__")) {
                continue;
            }
            ProtectedRegion existing = entry.getValue();
            if (testRegion.getIntersectingRegions(java.util.List.of(existing)).contains(existing)) {
                return id;
            }
        }
        return null;
    }
}
