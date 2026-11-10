package pl.riftplots.service.reset;

import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import pl.riftplots.api.PlotResetCompleteEvent;
import pl.riftplots.api.PlotResetStartEvent;
import pl.riftplots.api.PlotStateChangeEvent;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotBounds;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.integration.worldedit.WorldEditHook;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.sign.PlotSignService;

import java.io.File;
import java.util.Objects;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PlotResetService {
    private final Plugin plugin;
    private final PluginConfiguration config;
    private final PlotManager plotManager;
    private final PlotSnapshotService snapshotService;
    private final WorldEditHook worldEditHook;
    private PlotSignService signService;
    private pl.riftplots.service.rental.RentalService rentalService;
    private pl.riftplots.service.auction.AuctionService auctionService;
    private final Set<String> activeResets = ConcurrentHashMap.newKeySet();

    public PlotResetService(Plugin plugin,
                            PluginConfiguration config,
                            PlotManager plotManager,
                            PlotSnapshotService snapshotService,
                            WorldEditHook worldEditHook) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
        this.config = Objects.requireNonNull(config, "config cannot be null");
        this.plotManager = Objects.requireNonNull(plotManager, "plotManager cannot be null");
        this.snapshotService = Objects.requireNonNull(snapshotService, "snapshotService cannot be null");
        this.worldEditHook = Objects.requireNonNull(worldEditHook, "worldEditHook cannot be null");
    }

    public void setSignService(PlotSignService signService) {
        this.signService = signService;
    }

    public void setMarketServices(pl.riftplots.service.rental.RentalService rentalService,
                                  pl.riftplots.service.auction.AuctionService auctionService) {
        this.rentalService = rentalService;
        this.auctionService = auctionService;
    }

    public boolean isResetting(String plotId) {
        return plotId != null && activeResets.contains(plotId.toLowerCase());
    }

    public boolean startReset(Plot plot, Consumer<Boolean> callback) {
        if (plot == null) {
            if (callback != null) callback.accept(false);
            return false;
        }

        String plotId = plot.getId().toLowerCase();
        String originatingOperation = plotManager.currentOperationId();
        if (plotManager.financiallyBlocked(plotId, originatingOperation)) {
            if (callback != null) callback.accept(false);
            return false;
        }
        if (!activeResets.add(plotId)) {
            if (callback != null) callback.accept(false);
            return false;
        }

        boolean scheduled = false;
        try {
            File schematicFile = snapshotService.getSchematicFile(plot.getId());
            if (schematicFile == null || !schematicFile.isFile() || schematicFile.length() == 0) {
                plugin.getLogger().severe("Brak schematu działki " + plotId + ". Reset przerwany; działka została wyłączona.");
                plot.setStatus(PlotStatus.DISABLED);
                plotManager.savePlot(plot);
                activeResets.remove(plotId);
                if (callback != null) callback.accept(false);
                return false;
            }

            PlotResetStartEvent startEvent = new PlotResetStartEvent(plot);
            Bukkit.getPluginManager().callEvent(startEvent);
            if (startEvent.isCancelled()) {
                activeResets.remove(plotId);
                if (callback != null) callback.accept(false);
                return false;
            }

            java.util.UUID originalOwner = plot.getOwnerUuid();
            PlotBounds originalBounds = plot.getBounds();
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                try {
                    Clipboard clipboard = worldEditHook.loadSnapshot(schematicFile);
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        boolean accepted = false;
                        try {
                            String problem = validateClipboard(plot, clipboard);
                            if (problem != null || plotManager.financiallyBlocked(plotId, originatingOperation) || !Objects.equals(originalOwner, plot.getOwnerUuid())
                                    || !originalBounds.equals(plot.getBounds())) {
                                plugin.getLogger().warning("Reset " + plotId + " odrzucony: " + (problem != null ? problem : "zmieniony stan działki"));
                                if (callback != null) callback.accept(false);
                                return;
                            }
                            if (rentalService != null && !rentalService.terminateRental(plotId, true, "Reset nieruchomości")) {
                                if (callback != null) callback.accept(false);
                                return;
                            }
                            if (auctionService != null && plot.isAuctionActive()) auctionService.cancelAuction(plotId);
                            plot.clearOwnership();
                            plot.setStatus(PlotStatus.RESETTING);
                            plotManager.savePlot(plot);
                            if (signService != null) signService.updateSignsForPlot(plot);
                            pasteClipboard(plot, clipboard, callback);
                            accepted = true;
                        } catch (RuntimeException e) {
                            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Nie można rozpocząć resetu " + plotId, e);
                            if (callback != null) callback.accept(false);
                        } finally {
                            if (!accepted) activeResets.remove(plotId);
                        }
                    });
                } catch (Exception e) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        try {
                            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Nie można odczytać schematu " + plotId, e);
                            if (callback != null) callback.accept(false);
                        } finally {
                            activeResets.remove(plotId);
                        }
                    });
                }
            });
            scheduled = true;
            return true;
        } finally {
            if (!scheduled) activeResets.remove(plotId);
        }
    }

    public String validateClipboard(Plot plot, Clipboard clipboard) {
        if (clipboard == null) return "Nie można odczytać schematu.";
        PlotBounds bounds = plot.getBounds();
        if (Bukkit.getWorld(bounds.getWorldName()) == null) return "Świat działki nie jest załadowany.";
        var region = clipboard.getRegion();
        return region.getWidth() != bounds.getWidthX() || region.getHeight() != bounds.getHeightY()
                || region.getLength() != bounds.getLengthZ() ? "Wymiary schematu nie pasują do granic działki." : null;
    }

    public void preview(Plot plot, Consumer<String> callback) {
        File file = snapshotService.getSchematicFile(plot.getId());
        if (file == null || !file.isFile()) {
            callback.accept("Brak schematu.");
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Clipboard clipboard = worldEditHook.loadSnapshot(file);
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept(validateClipboard(plot, clipboard)));
            } catch (Exception e) {
                Bukkit.getScheduler().runTask(plugin, () -> callback.accept("Nie można odczytać schematu: " + e.getMessage()));
            }
        });
    }

    private void pasteClipboard(Plot plot, Clipboard clipboard, Consumer<Boolean> callback) {
        PlotBounds bounds = plot.getBounds();
        World bukkitWorld = Bukkit.getWorld(bounds.getWorldName());
        if (bukkitWorld == null) {
            finishReset(plot, false, callback);
            return;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        var region = clipboard.getRegion();
        if (region.getWidth() != bounds.getWidthX() || region.getHeight() != bounds.getHeightY()
                || region.getLength() != bounds.getLengthZ()) {
            plugin.getLogger().severe("Wymiary schematu nie pasują do granic działki " + plot.getId());
            finishReset(plot, false, callback);
            return;
        }
        BlockVector3 offset = BlockVector3.at(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ())
                .subtract(region.getMinimumPoint());
        Iterator<BlockVector3> blocks = region.iterator();
        new BukkitRunnable() {
            @Override
            public void run() {
                try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
                    editSession.setSideEffectApplier(com.sk89q.worldedit.util.SideEffectSet.none());
                    int count = 0;
                    while (blocks.hasNext() && count++ < config.getResetBlockBatchSize()) {
                        BlockVector3 source = blocks.next();
                        editSession.setBlock(source.add(offset), clipboard.getFullBlock(source));
                    }
                } catch (Exception e) {
                    plugin.getLogger().severe("Błąd resetu działki " + plot.getId() + ": " + e.getMessage());
                    cancel();
                    finishReset(plot, false, callback);
                    return;
                }
                if (!blocks.hasNext()) {
                    cancel();
                    finishReset(plot, true, callback);
                }
            }
        }.runTaskTimer(plugin, 0L, config.getResetTickDelay());
    }

    private void finishReset(Plot plot, boolean success, Consumer<Boolean> callback) {
        activeResets.remove(plot.getId().toLowerCase());

        PlotStatus targetStatus = success ? PlotStatus.AVAILABLE : PlotStatus.DISABLED;
        PlotStatus currentStatus = plot.getStatus();

        PlotStateChangeEvent stateEvent = new PlotStateChangeEvent(plot, currentStatus, targetStatus);
        Bukkit.getPluginManager().callEvent(stateEvent);

        plot.setStatus(stateEvent.isCancelled() || stateEvent.getNewStatus() == null
                ? PlotStatus.DISABLED : stateEvent.getNewStatus());
        plotManager.savePlot(plot);

        if (signService != null) {
            signService.updateSignsForPlot(plot);
        }

        PlotResetCompleteEvent completeEvent = new PlotResetCompleteEvent(plot, success);
        Bukkit.getPluginManager().callEvent(completeEvent);

        if (callback != null) {
            callback.accept(success);
        }
    }

    public void resumePendingResets() {
        for (Plot plot : plotManager.getAllPlots()) {
            if (plot.getStatus() == PlotStatus.RESETTING) {
                startReset(plot, success -> {
                    plugin.getLogger().info("Wznowiono i ukonczono reset dzialki: " + plot.getId() + " (sukces: " + success + ")");
                });
            }
        }
    }
}
