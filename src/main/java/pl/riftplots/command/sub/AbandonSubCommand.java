package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.math.BigDecimal;
import java.util.*;

public class AbandonSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PaymentService paymentService;
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;

    public AbandonSubCommand(PlotManager plotManager, PaymentService paymentService, PluginConfiguration config, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.paymentService = paymentService;
        this.config = config;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "abandon";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_ABANDON;
    }

    @Override
    public String getUsage() {
        return "plots abandon [działka] [confirm]";
    }

    @Override
    public String getDescription() {
        return "Dobrowolnie oddaje działkę z możliwością częściowego zwrotu kosztów.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        Plot targetPlot = null;
        boolean confirmed = false;

        if (args.length > 0) {
            String plotId = args[0];
            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isPresent()) {
                targetPlot = opt.get();
                if (args.length > 1 && "confirm".equalsIgnoreCase(args[1])) {
                    confirmed = true;
                }
            } else if ("confirm".equalsIgnoreCase(args[0])) {
                confirmed = true;
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

        if (!targetPlot.isOwner(player.getUniqueId()) && !player.hasPermission(PermissionConfiguration.ADMIN_BYPASS)) {
            player.sendMessage(messageConfig.get("plot.not-owner", Map.of("plot_id", targetPlot.getId())));
            return true;
        }

        if (!confirmed) {
            BigDecimal refund = paymentService.calculateAbandonRefund(targetPlot);
            player.sendMessage(messageConfig.get("abandon.confirm-prompt", Map.of(
                    "name", targetPlot.getName(),
                    "plot_id", targetPlot.getId(),
                    "refund", config.formatMoney(refund)
            )));
            return true;
        }

        paymentService.abandonPlot(player, targetPlot);
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
            return List.of("confirm");
        }
        return Collections.emptyList();
    }
}
