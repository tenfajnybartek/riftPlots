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

    public RentSubCommand(PlotManager plotManager, RentalService rentalService) {
        this.plotManager = plotManager;
        this.rentalService = rentalService;
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
        return "plots rent <działka> [liczba_okresów]";
    }

    @Override
    public String getDescription() {
        return "Wynajmuje udostępnioną nieruchomość.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTa komenda jest dostępna tylko dla graczy.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUżycie: /" + getUsage());
            return true;
        }

        String plotId = args[0];
        Optional<Plot> opt = plotManager.getPlot(plotId);
        if (opt.isEmpty()) {
            player.sendMessage("§cDziałka o identyfikatorze " + plotId + " nie istnieje!");
            return true;
        }

        int periods = 1;
        if (args.length > 1) {
            try {
                periods = Math.max(1, Integer.parseInt(args[1]));
            } catch (NumberFormatException ignored) {}
        }

        rentalService.rentPlot(player, opt.get(), periods, true);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .filter(Plot::isRentalAvailable)
                    .filter(p -> !p.isRented())
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            return List.of("1", "2", "3", "7", "14", "30");
        }
        return Collections.emptyList();
    }
}
