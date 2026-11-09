package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.auction.AuctionService;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.math.BigDecimal;
import java.util.*;

public class AdminAuctionSubCommand implements SubCommand {
    private final AuctionService auctionService;
    private final PlotManager plotManager;

    public AdminAuctionSubCommand(AuctionService auctionService, PlotManager plotManager) {
        this.auctionService = auctionService;
        this.plotManager = plotManager;
    }

    @Override
    public String getName() {
        return "auction";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_BASE;
    }

    @Override
    public String getUsage() {
        return "plots admin auction start <działka> <cena_startowa> <przebicie> <czas_minuty> [kup_teraz]";
    }

    @Override
    public String getDescription() {
        return "Wystawia działkę miejską na licytację.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 5 || !"start".equalsIgnoreCase(args[0])) {
            sender.sendMessage("§cUżycie: /" + getUsage());
            return true;
        }

        String plotId = args[1].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(plotId);
        if (opt.isEmpty()) {
            sender.sendMessage("§cDziałka nie istnieje.");
            return true;
        }
        Plot plot = opt.get();

        try {
            BigDecimal startingPrice = new BigDecimal(args[2]);
            BigDecimal minInc = new BigDecimal(args[3]);
            int minutes = Integer.parseInt(args[4]);
            BigDecimal buyout = args.length > 5 ? new BigDecimal(args[5]) : null;

            boolean started = auctionService.startAuction(plot, plot.getOwnerUuid(), startingPrice, minInc, minutes, buyout);
            if (started) {
                AdventureUtil.sendMessage(sender, "<green>Rozpoczęto licytację działki <yellow>" + plotId + "</yellow> na czas <gold>" + minutes + " minut</gold>!</green>");
            } else {
                sender.sendMessage("§cNie można rozpocząć aukcji. Sprawdź stan działki, wynajem i podane kwoty/czas.");
            }
        } catch (Exception e) {
            sender.sendMessage("§cBłąd parametrów: " + e.getMessage());
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("start");
        } else if (args.length == 2) {
            return plotManager.getAllPlots().stream().map(Plot::getId).toList();
        }
        return Collections.emptyList();
    }
}
