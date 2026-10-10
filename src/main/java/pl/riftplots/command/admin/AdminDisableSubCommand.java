package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.sign.PlotSignService;

import java.util.*;

public class AdminDisableSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PlotSignService signService;
    private final MessageConfiguration messageConfig;

    public AdminDisableSubCommand(PlotManager plotManager, PlotSignService signService, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.signService = signService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "disable";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_DISABLE;
    }

    @Override
    public String getUsage() {
        return "plots admin disable <id>";
    }

    @Override
    public String getDescription() {
        return "Wyłącza działkę z użytku.";
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
        plot.setStatus(PlotStatus.DISABLED);
        plotManager.savePlot(plot);
        signService.updateSignsForPlot(plot);

        sender.sendMessage(messageConfig.get("admin.disabled", Map.of("plot_id", id)));
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
