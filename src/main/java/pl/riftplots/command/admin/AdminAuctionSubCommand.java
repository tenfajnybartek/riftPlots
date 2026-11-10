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
        return pl.riftplots.config.Texts.raw("text.admin-auction.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-auction.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length >= 3 && args[0].equalsIgnoreCase("cancel")) {
            if (args.length < 4 || !args[2].equalsIgnoreCase("confirm")) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-auction.uzycie-plots-admin-auction-cancel-confirm-anulowanie"));
                return true;
            }
            auctionService.cancelAuction(args[1], sender.getName(), String.join(" ", Arrays.copyOfRange(args, 3, args.length)));
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-auction.anulowano-aukcje-ewentualne-zwroty-trafiaja-do-trwaej"));
            return true;
        }
        if (args.length < 5 || !"start".equalsIgnoreCase(args[0])) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-auction.uzycie", getUsage()));
            return true;
        }

        String plotId = args[1].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(plotId);
        if (opt.isEmpty()) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-auction.dziaka-nie-istnieje"));
            return true;
        }
        Plot plot = opt.get();

        try {
            BigDecimal startingPrice = pl.riftplots.util.Money.parse(args[2]);
            BigDecimal minInc = pl.riftplots.util.Money.parse(args[3]);
            int minutes = Integer.parseInt(args[4]);
            BigDecimal buyout = args.length > 5 ? pl.riftplots.util.Money.parse(args[5]) : null;

            boolean started = auctionService.startAuction(plot, plot.getOwnerUuid(), startingPrice, minInc, minutes, buyout);
            if (started) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-auction.rozpoczeto-licytacje-dziaki-na-czas-minut", plotId, minutes));
            } else {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-auction.nie-mozna-rozpoczac-aukcji-sprawdz-stan-dziaki"));
            }
        } catch (Exception e) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-auction.bad-parametrow", e.getMessage()));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("start", "cancel");
        } else if (args.length == 2) {
            return plotManager.getAllPlots().stream().map(Plot::getId).toList();
        }
        return Collections.emptyList();
    }
}
