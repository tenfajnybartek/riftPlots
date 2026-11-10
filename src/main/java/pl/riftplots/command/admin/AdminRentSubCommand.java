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
        return pl.riftplots.config.Texts.raw("text.admin-rent.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-rent.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.uzycie", getUsage()));
            return true;
        }

        String action = args[0].toLowerCase();
        String plotId = args[1].toLowerCase();

        Optional<Plot> opt = plotManager.getPlot(plotId);
        if (opt.isEmpty()) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.dziaka-nie-istnieje"));
            return true;
        }
        Plot plot = opt.get();

        if ("offer".equals(action)) {
            if (args.length < 4) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.uzycie-plots-admin-rent-offer-kaucja"));
                return true;
            }
            try {
                BigDecimal price = pl.riftplots.util.Money.parse(args[2]);
                String unit = pl.riftplots.domain.rental.RentalPeriodUnit.valueOf(args[3].toUpperCase(java.util.Locale.ROOT)).name();
                BigDecimal deposit = args.length > 4 ? pl.riftplots.util.Money.parse(args[4]) : BigDecimal.ZERO;
                if (price.signum() < 0 || deposit.signum() < 0 || plot.isRented() || plot.isAuctionActive()
                        || (plot.getStatus() != pl.riftplots.domain.plot.PlotStatus.AVAILABLE
                        && plot.getStatus() != pl.riftplots.domain.plot.PlotStatus.OWNED)) {
                    sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.kwoty-musza-byc-nieujemne-a-dziaka-dostepna"));
                    return true;
                }

                plot.setRentalAvailable(true);
                plot.setRentalPrice(price);
                plot.setRentalPeriodUnit(unit);
                plot.setRentalDeposit(deposit);
                plotManager.savePlot(plot);

                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.udostepniono-dziake-do-wynajmu-kaucja", plotId, price, unit, deposit));
            } catch (Exception e) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.bedne-parametry", e.getMessage()));
            }
            return true;
        }

        if ("cancel".equals(action)) {
            plot.setRentalAvailable(false);
            if (plot.isRented()) {
                if (!rentalService.terminateRental(plotId, true, pl.riftplots.config.Texts.raw("text.admin-rent.anulowanie-przez-administratora"))) {
                    sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.nie-udao-sie-zwrocic-kaucji-sprobuj-ponownie"));
                    return true;
                }
            }
            plotManager.savePlot(plot);
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.wyaczono-wynajem-dziaki", plotId));
            return true;
        }

        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-rent.uzycie", getUsage()));
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
