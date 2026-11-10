package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
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

public class AdminInfoSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;
    private final TaxCalculator taxCalculator;

    public AdminInfoSubCommand(PlotManager plotManager, PluginConfiguration config, MessageConfiguration messageConfig, TaxCalculator taxCalculator) {
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
        return PermissionConfiguration.ADMIN_INFO;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-info.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-info.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        String id = args[0].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(id);
        if (opt.isEmpty()) {
            sender.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", id)));
            return true;
        }

        Plot plot = opt.get();
        BigDecimal tax = taxCalculator.calculateTax(plot);

        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.panel-techniczny-dziaki", plot.getId().toUpperCase()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.nazwa", plot.getName()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.kategoria", plot.getCategory()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.status", plot.getStatus()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.region-worldguard", plot.getWorldGuardRegionId()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.granice", plot.getBounds()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.wymiary-x-x-pow-m2-chunki", plot.getBounds().getWidthX(), plot.getBounds().getHeightY(), plot.getBounds().getLengthZ(), plot.getBounds().getHorizontalArea(), plot.getBounds().getChunksCount()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.cena-bazowa", config.formatMoney(plot.getPrice())));
        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-info.mnoznik-cen-dzielnicy-cena-koncowa-reczne-nadpisania", plotManager.getDistrictPriceMultiplier(plot), config.formatMoney(plotManager.getPurchasePrice(plot)), plot.getSettingOverrides()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.podatek-bazowy", config.formatMoney(plot.getBaseTax())));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.tryb-podatku-mnoznik-waczony", plot.getTaxMode(), plot.getTaxMultiplier(), plot.isTaxEnabled()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.wyliczony-podatek", config.formatMoney(tax)));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.okres-rozliczeniowy-dni", plot.getBillingPeriodDays()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.czas-karencji-godz", plot.getGracePeriodHours()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.wasciciel", (plot.getOwnerName() != null ? plot.getOwnerName() : pl.riftplots.config.Texts.raw("text.admin-info.brak")), plot.getOwnerUuid()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.data-zakupu", TimeUtil.formatInstant(plot.getPurchaseDate())));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.termin-podatku", TimeUtil.formatInstant(plot.getNextTaxPayment())));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.wygasniecie-karencji", TimeUtil.formatInstant(plot.getGracePeriodExpiry())));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.opacone-z-gory", plot.getAdvancePeriodsPaid()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.plik-schematu", plot.getSchematicFile()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.auto-reset", plot.isAutoResetOnExpire()));
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-info.zwrot-przy-porzuceniu", plot.getRefundPercentOnAbandon()));

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
