package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.*;
import pl.riftplots.integration.worldedit.WorldEditHook;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.plot.PlotValidationResult;
import pl.riftplots.service.reset.PlotSnapshotService;

import java.time.Instant;
import java.util.*;

public class AdminCreateSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final WorldEditHook worldEditHook;
    private final PlotSnapshotService snapshotService;
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;

    public AdminCreateSubCommand(PlotManager plotManager,
                                 WorldEditHook worldEditHook,
                                 PlotSnapshotService snapshotService,
                                 PluginConfiguration config,
                                 MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.worldEditHook = worldEditHook;
        this.snapshotService = snapshotService;
        this.config = config;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "create";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_CREATE;
    }

    @Override
    public String getUsage() {
        return "plots admin create <id> [nazwa]";
    }

    @Override
    public String getDescription() {
        return "Tworzy nową działkę na podstawie zaznaczenia WorldEdit.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        String id = args[0].toLowerCase();
        PlotBounds bounds = worldEditHook.getPlayerSelection(player);
        if (bounds == null) {
            player.sendMessage(messageConfig.get("admin.selection-missing"));
            return true;
        }

        PlotValidationResult validation = plotManager.validateNewPlot(id, bounds);
        if (!validation.isValid()) {
            player.sendMessage(messageConfig.get("admin.collision-detected", Map.of("collision_id", validation.getErrorMessage())));
            return true;
        }

        String name = args.length > 1 ? String.join(" ", Arrays.copyOfRange(args, 1, args.length)) : id;
        Instant now = Instant.now();

        Plot plot = new Plot(
                id, name, "", PlotCategory.RESIDENTIAL, PlotStatus.AVAILABLE,
                bounds, "riftplot_" + id,
                config.getDefaultPrice(), config.getDefaultBaseTax(),
                config.getDefaultTaxMode(), config.getDefaultTaxMultiplier(),
                true, config.getDefaultBillingPeriodDays(), config.getDefaultGracePeriodHours(),
                null, null, Collections.emptyList(), null, null, null, 0,
                id + ".schem", config.isAutoResetOnExpire(), config.getDefaultAbandonRefundPercent(),
                now, now
        );

        boolean registered = plotManager.registerPlot(plot);
        if (!registered) {
            player.sendMessage(messageConfig.get("admin.worldguard-error", Map.of("error", "Nie udało się utworzyć regionu WorldGuard")));
            return true;
        }

        try {
            snapshotService.createSnapshot(plot);
        } catch (Exception e) {
            player.sendMessage(messageConfig.get("admin.worldguard-error", Map.of("error", "Błąd zapisu snapshotu: " + e.getMessage())));
        }

        player.sendMessage(messageConfig.get("admin.created", Map.of(
                "plot_id", id,
                "area", String.valueOf(bounds.getHorizontalArea())
        )));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
