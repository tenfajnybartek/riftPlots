package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.auction.PlotAuction;
import pl.riftplots.service.auction.AuctionService;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.math.BigDecimal;
import java.util.*;

public class AuctionSubCommand implements SubCommand {
    private final AuctionService auctionService;

    public AuctionSubCommand(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @Override
    public String getName() {
        return "auction";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_AUCTION;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.auction.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.auction.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("history")) {
            int page;
            try {
                page = args.length > 2 ? pl.riftplots.command.BillingArguments.positiveInt(args[2]) - 1 : 0;
            } catch (IllegalArgumentException e) {
                sender.sendMessage(e.getMessage());
                return true;
            }
            for (var bid : auctionService.getBidHistory(args[1], page))
                sender.sendMessage(pl.riftplots.config.Texts.component("text.auction.line", bid.getBidderName(), bid.getAmount(), bid.getTimestamp()));
            return true;
        }
        if (args.length >= 2 && args[0].equalsIgnoreCase("buy") && sender instanceof Player player) {
            var auction = auctionService.getAuction(args[1]).orElse(null);
            if (auction == null || auction.getBuyoutPrice() == null) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.auction.brak-aktywnej-ceny-kup-teraz"));
                return true;
            }
            if (args.length != 3 || !args[2].equalsIgnoreCase("confirm"))
                sender.sendMessage(pl.riftplots.config.Texts.component("text.auction.kup-teraz-potwierdz-plots-auction-buy-confirm", auction.getBuyoutPrice(), args[1]));
            else auctionService.placeBid(player, args[1], auction.getBuyoutPrice());
            return true;
        }
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            Collection<PlotAuction> list = auctionService.getActiveAuctions();
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.auction.aktywne-licytacje-dziaek"));
            if (list.isEmpty()) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.auction.brak-aktywnych-aukcji-w-tej-chwili"));
                return true;
            }
            for (PlotAuction a : list) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.auction.aktualna-oferta-lider-koniec-za", a.getPlotId(), a.getCurrentBid(), (a.getCurrentBidderName() != null ? a.getCurrentBidderName() : pl.riftplots.config.Texts.raw("text.auction.brak")), TimeUtil.formatRemainingTime(a.getEndTime())));
            }
            return true;
        }

        if ("bid".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.auction.ta-komenda-jest-dostepna-tylko-dla-graczy"));
                return true;
            }
            if (args.length < 3) {
                player.sendMessage(pl.riftplots.config.Texts.component("text.auction.uzycie-plots-auction-bid"));
                return true;
            }
            String plotId = args[1];
            BigDecimal amount;
            try {
                amount = pl.riftplots.util.Money.parse(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage(pl.riftplots.config.Texts.component("text.auction.podana-kwota-jest-nieprawidowa"));
                return true;
            }

            auctionService.placeBid(player, plotId, amount);
            return true;
        }

        sender.sendMessage(pl.riftplots.config.Texts.component("text.auction.uzycie", getUsage()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("list", "bid", "buy", "history");
        } else if (args.length == 2 && "bid".equalsIgnoreCase(args[0])) {
            return auctionService.getActiveAuctions().stream().map(PlotAuction::getPlotId).toList();
        }
        return Collections.emptyList();
    }
}
