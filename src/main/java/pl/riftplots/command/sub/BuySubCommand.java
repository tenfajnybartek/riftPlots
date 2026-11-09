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

public class BuySubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PaymentService paymentService;
    private final MessageConfiguration messageConfig;

    public BuySubCommand(PlotManager plotManager, PaymentService paymentService, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.paymentService = paymentService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "buy";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_BUY;
    }

    @Override
    public String getUsage() {
        return "plots buy [działka]";
    }

    @Override
    public String getDescription() {
        return "Kupuje przygotowaną działkę miejską.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        Plot targetPlot = null;
        if (args.length > 0) {
            String plotId = args[0];
            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isEmpty()) {
                player.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", plotId)));
                return true;
            }
            targetPlot = opt.get();
        } else {
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
            AdventureUtil.sendMessage(player, "<red>Podaj identyfikator działki lub stań na jej terenie, aby dokonać zakupu!</red>");
            return true;
        }

        paymentService.buyPlot(player, targetPlot);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .filter(Plot::isAvailableForPurchase)
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
