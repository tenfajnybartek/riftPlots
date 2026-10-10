package pl.riftplots.integration.worldedit;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.BlockArrayClipboard;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.*;
import com.sk89q.worldedit.function.operation.ForwardExtentCopy;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.regions.Region;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import pl.riftplots.domain.plot.PlotBounds;

import java.io.*;

public class WorldEditHook {
    private final Plugin plugin;

    public WorldEditHook(Plugin plugin) {
        this.plugin = plugin;
    }

    public boolean isAvailable() {
        return plugin.getServer().getPluginManager().getPlugin("WorldEdit") != null;
    }

    public PlotBounds getPlayerSelection(Player player) {
        if (!isAvailable() || player == null) {
            return null;
        }
        try {
            World world = BukkitAdapter.adapt(player.getWorld());
            Region region = WorldEdit.getInstance()
                    .getSessionManager()
                    .get(BukkitAdapter.adapt(player))
                    .getSelection(world);

            if (region == null) {
                return null;
            }

            BlockVector3 min = region.getMinimumPoint();
            BlockVector3 max = region.getMaximumPoint();
            return new PlotBounds(
                    player.getWorld().getName(),
                    min.x(), min.y(), min.z(),
                    max.x(), max.y(), max.z()
            );
        } catch (IncompleteRegionException e) {
            return null;
        }
    }

    public boolean saveSnapshot(PlotBounds bounds, File outputFile) throws IOException {
        if (!isAvailable() || bounds == null || outputFile == null) {
            return false;
        }
        org.bukkit.World bukkitWorld = Bukkit.getWorld(bounds.getWorldName());
        if (bukkitWorld == null) {
            throw new IllegalArgumentException("World " + bounds.getWorldName() + " does not exist!");
        }

        World world = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 min = BlockVector3.at(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
        BlockVector3 max = BlockVector3.at(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
        CuboidRegion region = new CuboidRegion(world, min, max);

        BlockArrayClipboard clipboard = new BlockArrayClipboard(region);
        clipboard.setOrigin(min);

        try {
            ForwardExtentCopy copy = new ForwardExtentCopy(world, region, clipboard, min);
            copy.setCopyingEntities(false);
            Operations.complete(copy);
        } catch (Exception e) {
            throw new IOException("Failed to copy extent for snapshot: " + e.getMessage(), e);
        }

        File parentDir = outputFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        ClipboardFormat format = ClipboardFormats.findByAlias("sponge.schem");
        if (format == null) {
            format = ClipboardFormats.findByAlias("schem");
        }
        if (format == null) {
            throw new IllegalStateException("No suitable schematic format found in WorldEdit!");
        }

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
             ClipboardWriter writer = format.getWriter(outputStream)) {
            writer.write(clipboard);
        }

        return true;
    }

    public Clipboard loadSnapshot(File inputFile) throws IOException {
        if (!isAvailable() || inputFile == null || !inputFile.exists()) {
            return null;
        }

        ClipboardFormat format = ClipboardFormats.findByFile(inputFile);
        if (format == null) {
            format = ClipboardFormats.findByAlias("sponge.schem");
        }
        if (format == null) {
            throw new IllegalStateException("Cannot determine clipboard format for " + inputFile.getName());
        }

        try (InputStream inputStream = new BufferedInputStream(new FileInputStream(inputFile));
             ClipboardReader reader = format.getReader(inputStream)) {
            return reader.read();
        }
    }
}
