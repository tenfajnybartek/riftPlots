package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.math.BigDecimal;
import java.util.*;

public class InfoSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;
    private final TaxCalculator taxCalculator;

    public InfoSubCommand(PlotManager plotManager, PluginConfiguration config, MessageConfiguration messageConfig, TaxCalculator taxCalculator) {
        this.plotManager = plotManager;
        this.config = config;
        this.messageConfig = messageConfig;
        this.taxCalculator = taxCalculator;
    }

    @Override
    public String getName() {
        return "info";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_INFO;
    }

    @Override
    public String getUsage() {
        return "plots info [działka]";
    }

    @Override
    public String getDescription() {
        return "Wyświetla szczegółowe informacje o wybranej działce.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Plot targetPlot = null;

        if (args.length > 0) {
            String plotId = args[0];
            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isEmpty()) {
                sender.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", plotId)));
                return true;
            }
            targetPlot = opt.get();
        } else if (sender instanceof Player player) {
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
            AdventureUtil.sendMessage(sender, "<red>Musisz podać identyfikator działki lub stać na jej terenie!</red>");
            return true;
        }

        BigDecimal tax = taxCalculator.calculateTax(targetPlot);

        AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== INFORMACJE O DZIAŁCE ===</b></gradient>");
        AdventureUtil.sendMessage(sender, "<gray>ID:</gray> <white>" + targetPlot.getId() + "</white>");
        AdventureUtil.sendMessage(sender, "<gray>Nazwa:</gray> <gold>" + targetPlot.getName() + "</gold>");
        AdventureUtil.sendMessage(sender, "<gray>Kategoria:</gray> <yellow>" + targetPlot.getCategory() + "</yellow>");
        AdventureUtil.sendMessage(sender, "<gray>Status:</gray> <aqua>" + targetPlot.getStatus() + "</aqua>");
        AdventureUtil.sendMessage(sender, "<gray>Świat:</gray> <white>" + targetPlot.getBounds().getWorldName() + "</white>");
        AdventureUtil.sendMessage(sender, "<gray>Powierzchnia:</gray> <white>" + targetPlot.getBounds().getHorizontalArea() + " m² (" + targetPlot.getBounds().getWidthX() + "x" + targetPlot.getBounds().getLengthZ() + ")</white>");
        AdventureUtil.sendMessage(sender, "<gray>Właściciel:</gray> <yellow>" + (targetPlot.getOwnerName() != null ? targetPlot.getOwnerName() : "Brak (Do kupienia)") + "</yellow>");
        AdventureUtil.sendMessage(sender, "<gray>Cena zakupu:</gray> <green>" + config.formatMoney(targetPlot.getPrice()) + "</green>");
        AdventureUtil.sendMessage(sender, "<gray>Podatek cykliczny:</gray> <gold>" + config.formatMoney(tax) + " / " + targetPlot.getBillingPeriodDays() + " dni (Tryb: " + targetPlot.getTaxMode() + ")</gold>");

        if (targetPlot.getOwnerUuid() != null) {
            AdventureUtil.sendMessage(sender, "<gray>Termin następnej płatności:</gray> <yellow>" + TimeUtil.formatInstant(targetPlot.getNextTaxPayment()) + " (pozostało: " + TimeUtil.formatRemainingTime(targetPlot.getNextTaxPayment()) + ")</yellow>");
            if (targetPlot.getGracePeriodExpiry() != null) {
                AdventureUtil.sendMessage(sender, "<red>Karencja upływa za:</red> <gold>" + TimeUtil.formatRemainingTime(targetPlot.getGracePeriodExpiry()) + "</gold>");
            }
            AdventureUtil.sendMessage(sender, "<gray>Współużytkownicy:</gray> <white>" + targetPlot.getMembers().size() + "</white>");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
