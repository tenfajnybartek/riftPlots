package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.TaxMode;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.sign.PlotSignService;

import java.util.*;

public class AdminSetTaxModeSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PlotSignService signService;
    private final MessageConfiguration messageConfig;

    public AdminSetTaxModeSubCommand(PlotManager plotManager, PlotSignService signService, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.signService = signService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "settaxmode";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_SETTAXMODE;
    }

    @Override
    public String getUsage() {
        return "plots admin settaxmode <id> <FIXED|PER_BLOCK|PER_CHUNK|SIZE_TIERS|MULTIPLIER|CUSTOM>";
    }

    @Override
    public String getDescription() {
        return "Zmienia model naliczania podatku dla działki.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        String id = args[0].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(id);
        if (opt.isEmpty()) {
            sender.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", id)));
            return true;
        }

        TaxMode mode;
        try {
            mode = TaxMode.valueOf(args[1].toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        Plot plot = opt.get();
        plot.setTaxMode(mode);
        plotManager.savePlot(plot);
        signService.updateSignsForPlot(plot);

        sender.sendMessage(messageConfig.get("admin.taxmode-set", Map.of(
                "plot_id", id,
                "mode", mode.name()
        )));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            return Arrays.stream(TaxMode.values()).map(Enum::name).toList();
        }
        return Collections.emptyList();
    }
}
