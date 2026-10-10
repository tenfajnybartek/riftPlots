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
        return "plots admin setprice <id> <cena>";
    }

    @Override
    public String getDescription() {
        return "Ustawia cenę zakupu działki.";
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
            price = new BigDecimal(args[1]);
            if (price.compareTo(BigDecimal.ZERO) < 0) {
                AdventureUtil.sendMessage(sender, "<red>Cena nie może być ujemna!</red>");
                return true;
            }
        } catch (NumberFormatException e) {
            AdventureUtil.sendMessage(sender, "<red>Podana kwota jest nieprawidłowa!</red>");
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
            return List.of("10000", "25000", "50000", "100000");
        }
        return Collections.emptyList();
    }
}
