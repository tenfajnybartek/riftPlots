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
        return pl.riftplots.config.Texts.raw("text.info.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.info.description");
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
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.musisz-podac-identyfikator-dziaki-lub-stac-na"));
            return true;
        }

        BigDecimal tax = taxCalculator.calculateTax(targetPlot);

        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.informacje-o-dziace"));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.id", targetPlot.getId()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.nazwa", targetPlot.getName()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.kategoria", targetPlot.getCategory()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.status", targetPlot.getStatus()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.swiat", targetPlot.getBounds().getWorldName()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.powierzchnia-m2-x", targetPlot.getBounds().getHorizontalArea(), targetPlot.getBounds().getWidthX(), targetPlot.getBounds().getLengthZ()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.wasciciel", (targetPlot.getOwnerName() != null ? targetPlot.getOwnerName() : pl.riftplots.config.Texts.raw("text.info.brak-do-kupienia"))));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.cena-zakupu", config.formatMoney(plotManager.getPurchasePrice(targetPlot))));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.podatek-cykliczny-dni-tryb", config.formatMoney(tax), targetPlot.getBillingPeriodDays(), targetPlot.getTaxMode()));

        if (targetPlot.getOwnerUuid() != null) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.termin-nastepnej-patnosci-pozostao", TimeUtil.formatInstant(targetPlot.getNextTaxPayment()), TimeUtil.formatRemainingTime(targetPlot.getNextTaxPayment())));
            if (targetPlot.getGracePeriodExpiry() != null) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.karencja-upywa-za", TimeUtil.formatRemainingTime(targetPlot.getGracePeriodExpiry())));
            }
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.info.wspouzytkownicy", targetPlot.getMembers().size()));
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
