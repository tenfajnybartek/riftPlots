package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.reset.PlotSnapshotService;

import java.io.File;
import java.util.*;

public class AdminSnapshotSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PlotSnapshotService snapshotService;
    private final MessageConfiguration messageConfig;

    public AdminSnapshotSubCommand(PlotManager plotManager, PlotSnapshotService snapshotService, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.snapshotService = snapshotService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "snapshot";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_SNAPSHOT;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-snapshot.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-snapshot.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        String id = args[0].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(id);
        if (opt.isEmpty()) {
            sender.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", id)));
            return true;
        }

        Plot plot = opt.get();
        try {
            boolean success = snapshotService.createSnapshot(plot);
            if (success) {
                File file = snapshotService.getSchematicFile(id);
                plotManager.savePlot(plot);
                sender.sendMessage(messageConfig.get("admin.snapshot-saved", Map.of(
                        "plot_id", id,
                        "file", file != null ? file.getName() : id + ".schem"
                )));
            } else {
                sender.sendMessage(messageConfig.get("admin.worldguard-error", Map.of("error", pl.riftplots.config.Texts.raw("text.admin-snapshot.nie-udao-sie-utworzyc-schematu-worldedit"))));
            }
        } catch (Exception e) {
            sender.sendMessage(messageConfig.get("admin.worldguard-error", Map.of("error", e.getMessage())));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
