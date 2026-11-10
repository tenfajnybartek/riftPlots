package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.rental.RentalService;

import java.math.BigDecimal;
import java.util.*;

public class AdminRentSubCommand implements SubCommand {
    private final RentalService rentalService;
    private final PlotManager plotManager;

    public AdminRentSubCommand(RentalService rentalService, PlotManager plotManager) {
        this.rentalService = rentalService;
        this.plotManager = plotManager;
    }

    @Override
    public String getName() {
        return "rent";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_BASE;
    }

    @Override
    public String getUsage() {
        return "plots admin rent <offer <działka> <cena> <jednostka: HOUR|DAY|WEEK|MONTH> [kaucja]|cancel <działka>>";
    }

    @Override
    public String getDescription() {
        return "Konfiguracja wynajmu działek przez administrację.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUżycie: /" + getUsage());
            return true;
        }

        String action = args[0].toLowerCase();
        String plotId = args[1].toLowerCase();

        Optional<Plot> opt = plotManager.getPlot(plotId);
        if (opt.isEmpty()) {
            sender.sendMessage("§cDziałka nie istnieje.");
            return true;
        }
        Plot plot = opt.get();

        if ("offer".equals(action)) {
            if (args.length < 4) {
                sender.sendMessage("§cUżycie: /plots admin rent offer <działka> <cena> <HOUR|DAY|WEEK|MONTH> [kaucja]");
                return true;
            }
            try {
                BigDecimal price = pl.riftplots.util.Money.parse(args[2]);
                String unit = pl.riftplots.domain.rental.RentalPeriodUnit.valueOf(args[3].toUpperCase(java.util.Locale.ROOT)).name();
                BigDecimal deposit = args.length > 4 ? pl.riftplots.util.Money.parse(args[4]) : BigDecimal.ZERO;
                if (price.signum() < 0 || deposit.signum() < 0 || plot.isRented() || plot.isAuctionActive()
                        || (plot.getStatus() != pl.riftplots.domain.plot.PlotStatus.AVAILABLE
                        && plot.getStatus() != pl.riftplots.domain.plot.PlotStatus.OWNED)) {
                    sender.sendMessage("§cKwoty muszą być nieujemne, a działka dostępna i bez aktywnego najmu lub aukcji.");
                    return true;
                }

                plot.setRentalAvailable(true);
                plot.setRentalPrice(price);
                plot.setRentalPeriodUnit(unit);
                plot.setRentalDeposit(deposit);
                plotManager.savePlot(plot);

                sender.sendMessage("§aUdostępniono działkę " + plotId + " do wynajmu (" + price + " $ / " + unit + ", kaucja: " + deposit + " $).");
            } catch (Exception e) {
                sender.sendMessage("§cBłędne parametry: " + e.getMessage());
            }
            return true;
        }

        if ("cancel".equals(action)) {
            plot.setRentalAvailable(false);
            if (plot.isRented()) {
                if (!rentalService.terminateRental(plotId, true, "Anulowanie przez administratora")) {
                    sender.sendMessage("§cNie udało się zwrócić kaucji. Spróbuj ponownie.");
                    return true;
                }
            }
            plotManager.savePlot(plot);
            sender.sendMessage("§aWyłączono wynajem działki " + plotId + ".");
            return true;
        }

        sender.sendMessage("§cUżycie: /" + getUsage());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("offer", "cancel");
        } else if (args.length == 2) {
            return plotManager.getAllPlots().stream().map(Plot::getId).toList();
        } else if (args.length == 4 && "offer".equalsIgnoreCase(args[0])) {
            return List.of("HOUR", "DAY", "WEEK", "MONTH");
        }
        return Collections.emptyList();
    }
}
