package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class PaySubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PaymentService paymentService;
    private final MessageConfiguration messageConfig;

    public PaySubCommand(PlotManager plotManager, PaymentService paymentService, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.paymentService = paymentService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "pay";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_PAY;
    }

    @Override
    public String getUsage() {
        return "plots pay [działka] [liczba_okresów]";
    }

    @Override
    public String getDescription() {
        return "Opłaca podatek za działkę na bieżący lub kolejne okresy.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        Plot targetPlot = null;
        int periods = 1;

        if (args.length > 0) {
            String plotId = args[0];
            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isPresent()) {
                targetPlot = opt.get();
                if (args.length > 1) {
                    try {
                        periods = Math.max(1, Integer.parseInt(args[1]));
                    } catch (NumberFormatException ignored) {}
                }
            } else {
                try {
                    periods = Math.max(1, Integer.parseInt(args[0]));
                } catch (NumberFormatException ignored) {}
            }
        }

        if (targetPlot == null) {
            Optional<Plot> opt = plotManager.getPlotAt(
                    player.getWorld().getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ()
            );
            if (opt.isPresent()) {
                targetPlot = opt.get();
            }
        }

        if (targetPlot == null) {
            AdventureUtil.sendMessage(player, "<red>Podaj identyfikator działki lub stań na jej terenie!</red>");
            return true;
        }

        paymentService.payTax(player, targetPlot, periods);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return plotManager.getPlotsByOwner(player.getUniqueId()).stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            return List.of("1", "2", "3", "4");
        }
        return Collections.emptyList();
    }
}
