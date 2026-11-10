package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.rental.RentalService;

import java.util.*;

public class RentSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final RentalService rentalService;
    private final pl.riftplots.config.MessageConfiguration messageConfig;

    public RentSubCommand(PlotManager plotManager, RentalService rentalService, pl.riftplots.config.MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.rentalService = rentalService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "rent";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_RENT;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.rent.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.rent.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        try {
            if (args[0].equalsIgnoreCase("info") && args.length == 2) {
                var agreement = rentalService.getActiveRental(args[1]).orElse(null);
                var plot = plotManager.getPlot(args[1]).orElse(null);
                if (agreement == null || !(agreement.getTenantUuid().equals(player.getUniqueId())
                        || plot != null && plot.isOwner(player.getUniqueId()) || player.hasPermission("riftplots.admin"))) {
                    player.sendMessage(pl.riftplots.config.Texts.component("text.rent.brak-dostepnej-umowy"));
                    return true;
                }
                player.sendMessage(pl.riftplots.config.Texts.component("text.rent.umowa-czynsz-kaucja-do-auto-odnowienie", agreement.getId(), agreement.getRentPrice(), agreement.getPeriodUnit(), agreement.getDeposit(), agreement.getEndDate(), agreement.isAutoRenew()));
                return true;
            }
            if (args[0].equalsIgnoreCase("autorenew") && args.length == 3) {
                if (!args[2].equalsIgnoreCase("on") && !args[2].equalsIgnoreCase("off"))
                    throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.rent.wybierz-on-albo-off"));
                player.sendMessage(rentalService.setAutoRenew(player, args[1], args[2].equalsIgnoreCase("on"))
                        ? pl.riftplots.config.Texts.raw("text.rent.zapisano-ustawienie-odnowienia") : pl.riftplots.config.Texts.raw("text.rent.nie-mozesz-zmienic-tej-umowy"));
                return true;
            }
            if (args[0].equalsIgnoreCase("cancel") && args.length >= 2 && args.length <= 3) {
                var agreement = rentalService.getActiveRental(args[1]).orElse(null);
                if (agreement == null || !agreement.getTenantUuid().equals(player.getUniqueId())) {
                    player.sendMessage(pl.riftplots.config.Texts.component("text.rent.nie-jestes-najemca"));
                    return true;
                }
                if (args.length != 3 || !args[2].equalsIgnoreCase("confirm")) {
                    player.sendMessage(pl.riftplots.config.Texts.component("text.rent.zakonczenie-odbiera-dostep-od-razu-opacony-czynsz", agreement.getDeposit(), agreement.getPlotId()));
                    return true;
                }
                player.sendMessage(rentalService.cancelByTenant(player, args[1]) ? pl.riftplots.config.Texts.raw("text.rent.zakonczono-najem-zwrot-kaucji-jest-rozliczany") : pl.riftplots.config.Texts.raw("text.rent.nie-mozna-zakonczyc-umowy"));
                return true;
            }
            if (args[0].equalsIgnoreCase("offer")) {
                if (!player.hasPermission("riftplots.rent.offer")) {
                    player.sendMessage(pl.riftplots.config.Texts.component("text.rent.brak-uprawnienia-riftplots-rent-offer"));
                    return true;
                }
                if (args.length != 6 || !args[5].equalsIgnoreCase("confirm"))
                    throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.rent.uzycie-plots-rent-offer-confirm"));
                var price = pl.riftplots.util.Money.parse(args[2]);
                var unit = pl.riftplots.domain.rental.RentalPeriodUnit.valueOf(args[3].toUpperCase(Locale.ROOT));
                var deposit = pl.riftplots.util.Money.parse(args[4]);
                player.sendMessage(rentalService.offerRental(player, plotManager.getPlot(args[1]).orElse(null), price, unit, deposit)
                        ? pl.riftplots.config.Texts.raw("text.rent.wystawiono-oferte-najmu") : pl.riftplots.config.Texts.raw("text.rent.nie-mozna-wystawic-tego-lokalu"));
                return true;
            }
            if (args.length > 3)
                throw new IllegalArgumentException(pl.riftplots.config.Texts.raw("text.rent.zbyt-wiele-argumentow"));
        } catch (IllegalArgumentException e) {
            player.sendMessage(net.kyori.adventure.text.Component.text(e.getMessage()));
            return true;
        }

        String plotId = args[0];
        Optional<Plot> opt = plotManager.getPlot(plotId);
        if (opt.isEmpty()) {
            player.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", plotId)));
            return true;
        }

        int periods = pl.riftplots.config.Settings.integer("rental.default-periods");
        if (args.length > 1) {
            try {
                periods = pl.riftplots.command.BillingArguments.positiveInt(args[1]);
            } catch (IllegalArgumentException e) {
                player.sendMessage(e.getMessage());
                return true;
            }
        }
        boolean autoRenew = pl.riftplots.config.Settings.flag("rental.default-auto-renew");
        if (args.length == 3) {
            if (!args[2].equalsIgnoreCase("on") && !args[2].equalsIgnoreCase("off")) {
                player.sendMessage(pl.riftplots.config.Texts.component("text.rent.wybierz-on-albo-off-dla-auto-odnowienia"));
                return true;
            }
            autoRenew = args[2].equalsIgnoreCase("on");
        }
        if (periods > rentalService.getMaxRentalPeriods()) {
            player.sendMessage(pl.riftplots.config.Texts.component("text.rent.przekroczono-limit-okresow-najmu"));
            return true;
        }
        rentalService.rentPlot(player, opt.get(), periods, autoRenew);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            List<String> result = new ArrayList<>(List.of("info", "autorenew", "cancel"));
            if (sender.hasPermission("riftplots.rent.offer")) result.add("offer");
            result.addAll(plotManager.getAllPlots().stream()
                    .filter(Plot::isRentalAvailable)
                    .filter(p -> !p.isRented())
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList());
            return result.stream().filter(value -> value.toLowerCase().startsWith(args[0].toLowerCase())).toList();
        } else if (args.length == 2) {
            return pl.riftplots.config.Settings.strings("tab-completion.rental-periods");
        }
        return Collections.emptyList();
    }
}
