package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.transfer.TransferOffer;
import pl.riftplots.service.transfer.PlotTransferService;

import java.util.Collections;
import java.util.List;

public class DenySubCommand implements SubCommand {
    private final PlotTransferService transferService;

    public DenySubCommand(PlotTransferService transferService) {
        this.transferService = transferService;
    }

    @Override
    public String getName() {
        return "deny";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_BASE;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.deny.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.deny.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.deny.ta-komenda-jest-dostepna-tylko-dla-graczy"));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(pl.riftplots.config.Texts.component("text.deny.uzycie", getUsage()));
            return true;
        }

        String offerId = args[0];
        transferService.cancelOffer(player, offerId);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return transferService.getOffersForBuyer(player.getUniqueId()).stream()
                    .map(TransferOffer::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
