package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.util.AdventureUtil;

import java.math.BigDecimal;
import java.util.*;

public class AdminSetPriceSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PlotSignService signService;
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;

    public AdminSetPriceSubCommand(PlotManager plotManager, PlotSignService signService, PluginConfiguration config, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.signService = signService;
        this.config = config;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "setprice";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_SETPRICE;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-set-price.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-set-price.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        String id = args[0].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(id);
        if (opt.isEmpty()) {
            sender.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", id)));
            return true;
        }

        BigDecimal price;
        try {
            price = pl.riftplots.util.Money.parse(args[1]);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-set-price.cena-nie-moze-byc-ujemna"));
                return true;
            }
        } catch (NumberFormatException e) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-set-price.podana-kwota-jest-nieprawidowa"));
            return true;
        }

        Plot plot = opt.get();
        plot.setPrice(price);
        plotManager.savePlot(plot);
        signService.updateSignsForPlot(plot);

        sender.sendMessage(messageConfig.get("admin.price-set", Map.of(
                "plot_id", id,
                "price", config.formatMoney(price)
        )));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            return pl.riftplots.config.Settings.strings("tab-completion.plot-prices");
        }
        return Collections.emptyList();
    }
}
