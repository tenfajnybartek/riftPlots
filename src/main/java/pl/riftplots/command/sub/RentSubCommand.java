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
        return "plots rent <id> [okresy] [on|off] | info <id> | autorenew <id> on|off | cancel <id> [confirm] | offer <id> <czynsz> <HOUR|DAY|WEEK|MONTH> <kaucja> confirm";
    }

    @Override
    public String getDescription() {
        return "Wynajmuje udostępnioną nieruchomość.";
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
                    player.sendMessage("Brak dostępnej umowy.");
                    return true;
                }
                player.sendMessage(net.kyori.adventure.text.Component.text("Umowa " + agreement.getId() + " | czynsz " + agreement.getRentPrice()
                        + " / " + agreement.getPeriodUnit() + " | kaucja " + agreement.getDeposit() + " | do " + agreement.getEndDate()
                        + " | auto-odnowienie: " + agreement.isAutoRenew()));
                return true;
            }
            if (args[0].equalsIgnoreCase("autorenew") && args.length == 3) {
                if (!args[2].equalsIgnoreCase("on") && !args[2].equalsIgnoreCase("off"))
                    throw new IllegalArgumentException("Wybierz on albo off.");
                player.sendMessage(rentalService.setAutoRenew(player, args[1], args[2].equalsIgnoreCase("on"))
                        ? "Zapisano ustawienie odnowienia." : "Nie możesz zmienić tej umowy.");
                return true;
            }
            if (args[0].equalsIgnoreCase("cancel") && args.length >= 2 && args.length <= 3) {
                var agreement = rentalService.getActiveRental(args[1]).orElse(null);
                if (agreement == null || !agreement.getTenantUuid().equals(player.getUniqueId())) {
                    player.sendMessage("Nie jesteś najemcą.");
                    return true;
                }
                if (args.length != 3 || !args[2].equalsIgnoreCase("confirm")) {
                    player.sendMessage("Zakończenie odbiera dostęp od razu. Opłacony czynsz nie jest zwracany. Kaucja: " + agreement.getDeposit()
                            + ". Potwierdź: /plots rent cancel " + agreement.getPlotId() + " confirm");
                    return true;
                }
                player.sendMessage(rentalService.cancelByTenant(player, args[1]) ? "Zakończono najem. Zwrot kaucji jest rozliczany." : "Nie można zakończyć umowy.");
                return true;
            }
            if (args[0].equalsIgnoreCase("offer")) {
                if (!player.hasPermission("riftplots.rent.offer")) {
                    player.sendMessage("Brak uprawnienia riftplots.rent.offer.");
                    return true;
                }
                if (args.length != 6 || !args[5].equalsIgnoreCase("confirm"))
                    throw new IllegalArgumentException("Użycie: /plots rent offer <id> <czynsz> <HOUR|DAY|WEEK|MONTH> <kaucja> confirm");
                var price = pl.riftplots.util.Money.parse(args[2]);
                var unit = pl.riftplots.domain.rental.RentalPeriodUnit.valueOf(args[3].toUpperCase(Locale.ROOT));
                var deposit = pl.riftplots.util.Money.parse(args[4]);
                player.sendMessage(rentalService.offerRental(player, plotManager.getPlot(args[1]).orElse(null), price, unit, deposit)
                        ? "Wystawiono ofertę najmu." : "Nie można wystawić tego lokalu.");
                return true;
            }
            if (args.length > 3) throw new IllegalArgumentException("Zbyt wiele argumentów.");
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

        int periods = 1;
        if (args.length > 1) {
            try {
                periods = pl.riftplots.command.BillingArguments.positiveInt(args[1]);
            } catch (IllegalArgumentException e) {
                player.sendMessage(e.getMessage());
                return true;
            }
        }
        boolean autoRenew = false;
        if (args.length == 3) {
            if (!args[2].equalsIgnoreCase("on") && !args[2].equalsIgnoreCase("off")) {
                player.sendMessage("Wybierz on albo off dla auto-odnowienia.");
                return true;
            }
            autoRenew = args[2].equalsIgnoreCase("on");
        }
        if (periods > rentalService.getMaxRentalPeriods()) {
            player.sendMessage("Przekroczono limit okresów najmu.");
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
            return List.of("1", "2", "3", "7", "14", "30");
        }
        return Collections.emptyList();
    }
}
