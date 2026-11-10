package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminSetGraceSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final MessageConfiguration messageConfig;

    public AdminSetGraceSubCommand(PlotManager plotManager, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "setgrace";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_SETGRACE;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-set-grace.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-set-grace.description");
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

        int hours;
        try {
            hours = Integer.parseInt(args[1]);
            if (hours < 0) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-set-grace.liczba-godzin-nie-moze-byc-ujemna"));
                return true;
            }
        } catch (NumberFormatException e) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-set-grace.nieprawidowa-liczba-godzin"));
            return true;
        }

        Plot plot = opt.get();
        plot.setGracePeriodHours(hours);
        plotManager.savePlot(plot);

        sender.sendMessage(messageConfig.get("admin.grace-set", Map.of(
                "plot_id", id,
                "hours", String.valueOf(hours)
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
            return pl.riftplots.config.Settings.strings("tab-completion.grace-hours");
        }
        return Collections.emptyList();
    }
}
