package pl.riftplots.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.transfer.PlotTransferService;

import java.math.BigDecimal;
import java.util.*;

public class TransferSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PlotTransferService transferService;
    private final pl.riftplots.config.MessageConfiguration messageConfig;

    public TransferSubCommand(PlotManager plotManager, PlotTransferService transferService, pl.riftplots.config.MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.transferService = transferService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "transfer";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_SELL;
    }

    @Override
    public String getUsage() {
        return "plots transfer <gracz> <cena> [działka]";
    }

    @Override
    public String getDescription() {
        return "Wysyła ofertę sprzedaży lub przekazania działki innemu graczowi.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        Player buyer = Bukkit.getPlayer(args[0]);
        if (buyer == null || !buyer.isOnline()) {
            player.sendMessage(messageConfig.get("members.player-not-found", Map.of("target", args[0])));
            return true;
        }

        if (buyer.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(messageConfig.get("transfer.cannot-sell-self"));
            return true;
        }

        BigDecimal price;
        try {
            price = new BigDecimal(args[1]);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                player.sendMessage(messageConfig.get("budget.invalid-amount"));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(messageConfig.get("budget.invalid-amount"));
            return true;
        }

        Plot targetPlot = null;
        if (args.length > 2) {
            String plotId = args[2];
            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isPresent()) {
                targetPlot = opt.get();
            }
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
            player.sendMessage("§cPodaj identyfikator działki lub stań na jej terenie!");
            return true;
        }

        transferService.createOffer(targetPlot, player, buyer, price, 0);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        } else if (args.length == 2) {
            return List.of("0", "1000", "5000", "10000", "50000");
        } else if (args.length == 3 && sender instanceof Player player) {
            return plotManager.getPlotsByOwner(player.getUniqueId()).stream().map(Plot::getId).filter(id -> id.toLowerCase().startsWith(args[2].toLowerCase())).toList();
        }
        return Collections.emptyList();
    }
}
