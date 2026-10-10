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
        return "plots auction [list|bid <działka> <kwota>]";
    }

    @Override
    public String getDescription() {
        return "Przeglądanie i udział w licytacjach nieruchomości miejskich.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            Collection<PlotAuction> list = auctionService.getActiveAuctions();
            AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== AKTYWNE LICYTACJE DZIAŁEK ===</b></gradient>");
            if (list.isEmpty()) {
                AdventureUtil.sendMessage(sender, "<gray>Brak aktywnych aukcji w tej chwili.</gray>");
                return true;
            }
            for (PlotAuction a : list) {
                AdventureUtil.sendMessage(sender, "<yellow>" + a.getPlotId() + "</yellow> - <gray>Aktualna oferta: </gray><gold>" + a.getCurrentBid() + " $</gold> <gray>(Lider: <aqua>" + (a.getCurrentBidderName() != null ? a.getCurrentBidderName() : "Brak") + "</aqua>, Koniec za: <white>" + TimeUtil.formatRemainingTime(a.getEndTime()) + "</white>)</gray>");
            }
            return true;
        }

        if ("bid".equalsIgnoreCase(args[0])) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("§cTa komenda jest dostępna tylko dla graczy.");
                return true;
            }
            if (args.length < 3) {
                player.sendMessage("§cUżycie: /plots auction bid <działka> <kwota>");
                return true;
            }
            String plotId = args[1];
            BigDecimal amount;
            try {
                amount = new BigDecimal(args[2]);
            } catch (NumberFormatException e) {
                player.sendMessage("§cPodana kwota jest nieprawidłowa!");
                return true;
            }

            auctionService.placeBid(player, plotId, amount);
            return true;
        }

        sender.sendMessage("§cUżycie: /" + getUsage());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("list", "bid");
        } else if (args.length == 2 && "bid".equalsIgnoreCase(args[0])) {
            return auctionService.getActiveAuctions().stream().map(PlotAuction::getPlotId).toList();
        }
        return Collections.emptyList();
    }
}
