package pl.riftplots.listener;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.EntityExplodeEvent;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;

import java.util.List;
import java.util.Optional;

public class PlotPistonFluidListener implements Listener {
    private final PluginConfiguration config;
    private final PlotManager plotManager;

    public PlotPistonFluidListener(PluginConfiguration config, PlotManager plotManager) {
        this.config = config;
        this.plotManager = plotManager;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        if (!config.isProtectPistons()) {
            return;
        }

        Block piston = event.getBlock();
        String worldName = piston.getWorld().getName();
        Optional<Plot> pistonPlot = plotManager.getPlotAt(worldName, piston.getX(), piston.getY(), piston.getZ());

        Block head = piston.getRelative(event.getDirection());
        if (!plotsMatch(pistonPlot, plotManager.getPlotAt(worldName, head.getX(), head.getY(), head.getZ()))) {
            event.setCancelled(true);
            return;
        }

        for (Block movedBlock : event.getBlocks()) {
            Optional<Plot> sourcePlot = plotManager.getPlotAt(worldName, movedBlock.getX(), movedBlock.getY(), movedBlock.getZ());
            Location targetLoc = movedBlock.getLocation().add(event.getDirection().getDirection());
            Optional<Plot> targetPlot = plotManager.getPlotAt(worldName, targetLoc.getBlockX(), targetLoc.getBlockY(), targetLoc.getBlockZ());

            if (!plotsMatch(pistonPlot, sourcePlot) || !plotsMatch(pistonPlot, targetPlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (!config.isProtectPistons()) {
            return;
        }

        Block piston = event.getBlock();
        String worldName = piston.getWorld().getName();
        Optional<Plot> pistonPlot = plotManager.getPlotAt(worldName, piston.getX(), piston.getY(), piston.getZ());

        for (Block movedBlock : event.getBlocks()) {
            Optional<Plot> movedPlot = plotManager.getPlotAt(worldName, movedBlock.getX(), movedBlock.getY(), movedBlock.getZ());
            var facing = ((org.bukkit.block.data.Directional) piston.getBlockData()).getFacing();
            Block target = movedBlock.getRelative(facing.getOppositeFace());
            Optional<Plot> targetPlot = plotManager.getPlotAt(worldName, target.getX(), target.getY(), target.getZ());
            if (!plotsMatch(pistonPlot, movedPlot) || !plotsMatch(pistonPlot, targetPlot)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockFromTo(BlockFromToEvent event) {
        if (!config.isProtectFluids()) {
            return;
        }

        Block from = event.getBlock();
        Block to = event.getToBlock();
        String worldName = from.getWorld().getName();

        Optional<Plot> fromPlot = plotManager.getPlotAt(worldName, from.getX(), from.getY(), from.getZ());
        Optional<Plot> toPlot = plotManager.getPlotAt(worldName, to.getX(), to.getY(), to.getZ());

        if (!plotsMatch(fromPlot, toPlot)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!config.isProtectExplosions()) {
            return;
        }

        String worldName = event.getLocation().getWorld().getName();
        List<Block> blocks = event.blockList();
        blocks.removeIf(b -> plotManager.getPlotAt(worldName, b.getX(), b.getY(), b.getZ()).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!config.isProtectExplosions()) {
            return;
        }

        String worldName = event.getBlock().getWorld().getName();
        List<Block> blocks = event.blockList();
        blocks.removeIf(b -> plotManager.getPlotAt(worldName, b.getX(), b.getY(), b.getZ()).isPresent());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!config.isProtectFireSpread()) {
            return;
        }
        Block block = event.getBlock();
        if (plotManager.getPlotAt(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).isPresent()) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (!config.isProtectFireSpread()) {
            return;
        }
        Block block = event.getBlock();
        if (plotManager.getPlotAt(block.getWorld().getName(), block.getX(), block.getY(), block.getZ()).isPresent()) {
            if (event.getCause() == BlockIgniteEvent.IgniteCause.SPREAD || event.getCause() == BlockIgniteEvent.IgniteCause.LAVA) {
                event.setCancelled(true);
            }
        }
    }

    private boolean plotsMatch(Optional<Plot> p1, Optional<Plot> p2) {
        if (p1.isEmpty() && p2.isEmpty()) {
            return true;
        }
        if (p1.isPresent() && p2.isPresent()) {
            return p1.get().getId().equalsIgnoreCase(p2.get().getId());
        }
        return false;
    }
}
