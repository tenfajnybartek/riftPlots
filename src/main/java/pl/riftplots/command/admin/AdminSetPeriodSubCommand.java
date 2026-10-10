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
        return "plots admin setperiod <id> <dni>";
    }

    @Override
    public String getDescription() {
        return "Ustawia długość okresu rozliczeniowego w dniach.";
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
                AdventureUtil.sendMessage(sender, "<red>Liczba dni musi być większa od zera!</red>");
                return true;
            }
        } catch (NumberFormatException e) {
            AdventureUtil.sendMessage(sender, "<red>Nieprawidłowa liczba dni!</red>");
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
            return List.of("1", "3", "7", "14", "30");
        }
        return Collections.emptyList();
    }
}
