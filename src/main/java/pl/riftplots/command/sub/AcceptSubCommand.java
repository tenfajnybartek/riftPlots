package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.transfer.TransferOffer;
import pl.riftplots.service.transfer.PlotTransferService;

import java.util.Collections;
import java.util.List;

public class AcceptSubCommand implements SubCommand {
    private final PlotTransferService transferService;
    private final pl.riftplots.config.MessageConfiguration messageConfig;

    public AcceptSubCommand(PlotTransferService transferService, pl.riftplots.config.MessageConfiguration messageConfig) {
        this.transferService = transferService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "accept";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_BASE;
    }

    @Override
    public String getUsage() {
        return "plots accept <id_oferty>";
    }

    @Override
    public String getDescription() {
        return "Akceptuje otrzymaną ofertę zakupu lub przekazania działki.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        String offerId = null;
        if (args.length > 0) {
            offerId = args[0];
        } else {
            List<TransferOffer> offers = transferService.getOffersForBuyer(player.getUniqueId());
            if (!offers.isEmpty()) {
                offerId = offers.get(0).getId();
            }
        }

        if (offerId == null) {
            player.sendMessage(messageConfig.get("transfer.no-offers"));
            return true;
        }

        transferService.acceptOffer(player, offerId);
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
