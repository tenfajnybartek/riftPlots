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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ExtendSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PaymentService paymentService;
    private final MessageConfiguration messageConfig;

    public ExtendSubCommand(PlotManager plotManager, PaymentService paymentService, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.paymentService = paymentService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "extend";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_EXTEND;
    }

    @Override
    public String getUsage() {
        return "plots extend [działka] [liczba_okresów]";
    }

    @Override
    public String getDescription() {
        return "Przedłuża ważność opłat działki z góry.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        try {
            var billing = pl.riftplots.command.BillingArguments.resolve(player, args, plotManager);
            paymentService.payTax(player, billing.plot(), billing.periods());
        } catch (IllegalArgumentException e) {
            player.sendMessage(net.kyori.adventure.text.Component.text(e.getMessage()));
        }
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
