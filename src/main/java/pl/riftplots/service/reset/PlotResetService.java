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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PlotResetService {
    private final Plugin plugin;
    private final PluginConfiguration config;
    private final PlotManager plotManager;
    private final PlotSnapshotService snapshotService;
    private final WorldEditHook worldEditHook;
    private PlotSignService signService;
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

    public boolean isResetting(String plotId) {
        return plotId != null && activeResets.contains(plotId.toLowerCase());
    }

    public void startReset(Plot plot, Consumer<Boolean> callback) {
        if (plot == null) {
            if (callback != null) callback.accept(false);
            return;
        }

        String plotId = plot.getId().toLowerCase();
        if (!activeResets.add(plotId)) {
            if (callback != null) callback.accept(false);
            return;
        }

        PlotResetStartEvent startEvent = new PlotResetStartEvent(plot);
        Bukkit.getPluginManager().callEvent(startEvent);
        if (startEvent.isCancelled()) {
            activeResets.remove(plotId);
            if (callback != null) callback.accept(false);
            return;
        }

        PlotStatus oldStatus = plot.getStatus();
        plot.setStatus(PlotStatus.RESETTING);
        plot.setOwnerUuid(null);
        plot.setOwnerName(null);
        plot.clearMembers();
        plot.setPurchaseDate(null);
        plot.setNextTaxPayment(null);
        plot.setGracePeriodExpiry(null);
        plot.setAdvancePeriodsPaid(0);

        plotManager.savePlot(plot);

        if (signService != null) {
            signService.updateSignsForPlot(plot);
        }

        File schematicFile = snapshotService.getSchematicFile(plot.getId());
        if (schematicFile == null || !schematicFile.exists()) {
            finishReset(plot, true, callback);
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Clipboard clipboard = worldEditHook.loadSnapshot(schematicFile);
                if (clipboard == null) {
                    Bukkit.getScheduler().runTask(plugin, () -> finishReset(plot, false, callback));
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    boolean success = pasteClipboard(plot, clipboard);
                    finishReset(plot, success, callback);
                });
            } catch (Exception e) {
                e.printStackTrace();
                Bukkit.getScheduler().runTask(plugin, () -> finishReset(plot, false, callback));
            }
        });
    }

    private boolean pasteClipboard(Plot plot, Clipboard clipboard) {
        PlotBounds bounds = plot.getBounds();
        World bukkitWorld = Bukkit.getWorld(bounds.getWorldName());
        if (bukkitWorld == null) {
            return false;
        }

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(bukkitWorld);
        BlockVector3 pasteLocation = BlockVector3.at(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());

        try (EditSession editSession = WorldEdit.getInstance().newEditSession(weWorld)) {
            editSession.setSideEffectApplier(com.sk89q.worldedit.util.SideEffectSet.none());
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(editSession)
                    .to(pasteLocation)
                    .ignoreAirBlocks(false)
                    .copyEntities(false)
                    .copyBiomes(false)
                    .build();

            Operations.complete(operation);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void finishReset(Plot plot, boolean success, Consumer<Boolean> callback) {
        activeResets.remove(plot.getId().toLowerCase());

        PlotStatus targetStatus = success ? PlotStatus.AVAILABLE : PlotStatus.DISABLED;
        PlotStatus currentStatus = plot.getStatus();

        PlotStateChangeEvent stateEvent = new PlotStateChangeEvent(plot, currentStatus, targetStatus);
        Bukkit.getPluginManager().callEvent(stateEvent);

        plot.setStatus(stateEvent.getNewStatus());
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
