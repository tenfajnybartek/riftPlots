package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.reset.PlotResetService;

import java.util.*;

public class AdminRestoreSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PlotResetService resetService;
    private final MessageConfiguration messageConfig;

    public AdminRestoreSubCommand(PlotManager plotManager, PlotResetService resetService, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.resetService = resetService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "restore";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_RESTORE;
    }

    @Override
    public String getUsage() {
        return "plots admin restore <id>";
    }

    @Override
    public String getDescription() {
        return "Odtwarza stan pierwotny nieruchomości z zapisanego schematu.";
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
        sender.sendMessage(messageConfig.get("admin.restore-started", Map.of("plot_id", id)));

        resetService.startReset(plot, success -> {
            if (success) {
                sender.sendMessage(messageConfig.get("admin.restore-finished", Map.of("plot_id", id)));
            } else {
                sender.sendMessage(messageConfig.get("admin.worldguard-error", Map.of("error", "Błąd podczas odtwarzania stanu działki!")));
            }
        });

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
