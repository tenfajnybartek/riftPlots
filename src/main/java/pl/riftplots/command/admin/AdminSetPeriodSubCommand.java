package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminSetPeriodSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final MessageConfiguration messageConfig;

    public AdminSetPeriodSubCommand(PlotManager plotManager, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "setperiod";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_SETPERIOD;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-set-period.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-set-period.description");
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

        int days;
        try {
            days = Integer.parseInt(args[1]);
            if (days <= 0) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-set-period.liczba-dni-musi-byc-wieksza-od-zera"));
                return true;
            }
        } catch (NumberFormatException e) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-set-period.nieprawidowa-liczba-dni"));
            return true;
        }

        Plot plot = opt.get();
        plot.setBillingPeriodDays(days);
        plotManager.savePlot(plot);

        sender.sendMessage(messageConfig.get("admin.period-set", Map.of(
                "plot_id", id,
                "days", String.valueOf(days)
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
            return pl.riftplots.config.Settings.strings("tab-completion.billing-days");
        }
        return Collections.emptyList();
    }
}
