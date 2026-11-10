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
        return "plots admin info <id>";
    }

    @Override
    public String getDescription() {
        return "Wyświetla zaawansowane parametry techniczne działki.";
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

        AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== PANEL TECHNICZNY DZIAŁKI: " + plot.getId().toUpperCase() + " ===</b></gradient>");
        AdventureUtil.sendMessage(sender, "<gray>Nazwa: </gray><gold>" + plot.getName() + "</gold>");
        AdventureUtil.sendMessage(sender, "<gray>Kategoria: </gray><yellow>" + plot.getCategory() + "</yellow>");
        AdventureUtil.sendMessage(sender, "<gray>Status: </gray><aqua>" + plot.getStatus() + "</aqua>");
        AdventureUtil.sendMessage(sender, "<gray>Region WorldGuard: </gray><white>" + plot.getWorldGuardRegionId() + "</white>");
        AdventureUtil.sendMessage(sender, "<gray>Granice: </gray><white>" + plot.getBounds() + "</white>");
        AdventureUtil.sendMessage(sender, "<gray>Wymiary: </gray><white>" + plot.getBounds().getWidthX() + "x" + plot.getBounds().getHeightY() + "x" + plot.getBounds().getLengthZ() + " (Pow: " + plot.getBounds().getHorizontalArea() + " m², Chunki: " + plot.getBounds().getChunksCount() + ")</white>");
        AdventureUtil.sendMessage(sender, "<gray>Cena bazowa: </gray><green>" + config.formatMoney(plot.getPrice()) + "</green>");
        sender.sendMessage(net.kyori.adventure.text.Component.text("Mnożnik cen dzielnicy: " + plotManager.getDistrictPriceMultiplier(plot)
                + "; cena końcowa: " + config.formatMoney(plotManager.getPurchasePrice(plot))
                + "; ręczne nadpisania: " + plot.getSettingOverrides()));
        AdventureUtil.sendMessage(sender, "<gray>Podatek bazowy: </gray><gold>" + config.formatMoney(plot.getBaseTax()) + "</gold>");
        AdventureUtil.sendMessage(sender, "<gray>Tryb podatku: </gray><yellow>" + plot.getTaxMode() + " (Mnożnik: " + plot.getTaxMultiplier() + ", Włączony: " + plot.isTaxEnabled() + ")</yellow>");
        AdventureUtil.sendMessage(sender, "<gray>Wyliczony podatek: </gray><gold>" + config.formatMoney(tax) + "</gold>");
        AdventureUtil.sendMessage(sender, "<gray>Okres rozliczeniowy: </gray><white>" + plot.getBillingPeriodDays() + " dni</white>");
        AdventureUtil.sendMessage(sender, "<gray>Czas karencji: </gray><white>" + plot.getGracePeriodHours() + " godz.</white>");
        AdventureUtil.sendMessage(sender, "<gray>Właściciel: </gray><aqua>" + (plot.getOwnerName() != null ? plot.getOwnerName() : "Brak") + " (" + plot.getOwnerUuid() + ")</aqua>");
        AdventureUtil.sendMessage(sender, "<gray>Data zakupu: </gray><white>" + TimeUtil.formatInstant(plot.getPurchaseDate()) + "</white>");
        AdventureUtil.sendMessage(sender, "<gray>Termin podatku: </gray><yellow>" + TimeUtil.formatInstant(plot.getNextTaxPayment()) + "</yellow>");
        AdventureUtil.sendMessage(sender, "<gray>Wygaśnięcie karencji: </gray><red>" + TimeUtil.formatInstant(plot.getGracePeriodExpiry()) + "</red>");
        AdventureUtil.sendMessage(sender, "<gray>Opłacone z góry: </gray><aqua>" + plot.getAdvancePeriodsPaid() + "</aqua>");
        AdventureUtil.sendMessage(sender, "<gray>Plik schematu: </gray><white>" + plot.getSchematicFile() + "</white>");
        AdventureUtil.sendMessage(sender, "<gray>Auto-reset: </gray><yellow>" + plot.isAutoResetOnExpire() + "</yellow>");
        AdventureUtil.sendMessage(sender, "<gray>Zwrot przy porzuceniu: </gray><green>" + plot.getRefundPercentOnAbandon() + "%</green>");

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
