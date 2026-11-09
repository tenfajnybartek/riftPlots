package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.type.PropertyType;
import pl.riftplots.domain.type.PropertyTypeManager;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminTypeSubCommand implements SubCommand {
    private final PropertyTypeManager typeManager;
    private final PlotManager plotManager;

    public AdminTypeSubCommand(PropertyTypeManager typeManager, PlotManager plotManager) {
        this.typeManager = typeManager;
        this.plotManager = plotManager;
    }

    @Override
    public String getName() {
        return "type";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_TYPE;
    }

    @Override
    public String getUsage() {
        return "plots admin type <list|set <działka> <typ>|info <typ>>";
    }

    @Override
    public String getDescription() {
        return "Zarządzanie typami nieruchomości i ich przypisaniem.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== TYPY NIERUCHOMOŚCI ===</b></gradient>");
            for (PropertyType t : typeManager.getAllTypes()) {
                AdventureUtil.sendMessage(sender, "<yellow>" + t.getId() + "</yellow> - <gold>" + t.getDisplayName() + "</gold> <gray>(Cena bazowa: " + t.getDefaultPrice() + " $, Podatek: " + t.getDefaultBaseTax() + " $, Wynajem: " + t.isRentalAllowed() + ")</gray>");
            }
            return true;
        }

        if ("set".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage("§cUżycie: /plots admin type set <działka> <typ>");
                return true;
            }
            String plotId = args[1].toLowerCase();
            String typeId = args[2].toUpperCase();
            if (!typeManager.exists(typeId)) {
                sender.sendMessage("§cNieznany typ nieruchomości: " + typeId);
                return true;
            }

            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isEmpty()) {
                sender.sendMessage("§cDziałka nie istnieje.");
                return true;
            }

            PropertyType propType = typeManager.getType(typeId);
            Plot plot = opt.get();
            plot.setPropertyType(propType.getId());
            plotManager.savePlot(plot);

            sender.sendMessage("§aUstawiono typ działki " + plotId + " na: " + propType.getDisplayName() + " (" + propType.getId() + ").");
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage("§cUżycie: /plots admin type info <typ>");
                return true;
            }
            String typeId = args[1].toUpperCase();
            if (!typeManager.exists(typeId)) {
                sender.sendMessage("§cNieznany typ nieruchomości: " + typeId);
                return true;
            }
            PropertyType t = typeManager.getType(typeId);
            AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== INFORMACJE O TYPIE: " + t.getDisplayName() + " ===</b></gradient>");
            AdventureUtil.sendMessage(sender, "<gray>ID: </gray><white>" + t.getId() + "</white>");
            AdventureUtil.sendMessage(sender, "<gray>Opis: </gray><yellow>" + t.getDescription() + "</yellow>");
            AdventureUtil.sendMessage(sender, "<gray>Domyślna cena: </gray><green>" + t.getDefaultPrice() + " $</green>");
            AdventureUtil.sendMessage(sender, "<gray>Domyślny podatek: </gray><gold>" + t.getDefaultBaseTax() + " $ (Tryb: " + t.getDefaultTaxMode() + ")</gold>");
            AdventureUtil.sendMessage(sender, "<gray>Limit członków: </gray><white>" + t.getMaxMembers() + "</white>");
            AdventureUtil.sendMessage(sender, "<gray>Dozwolony wynajem: </gray><yellow>" + t.isRentalAllowed() + "</yellow> | <gray>Dozwolona sprzedaż: </gray><yellow>" + t.isSalesAllowed() + "</yellow>");
            return true;
        }

        sender.sendMessage("§cUżycie: /" + getUsage());
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("list", "set", "info");
        } else if (args.length == 2 && "set".equalsIgnoreCase(args[0])) {
            return plotManager.getAllPlots().stream().map(Plot::getId).toList();
        } else if (args.length == 3 && "set".equalsIgnoreCase(args[0])) {
            return typeManager.getAllTypes().stream().map(PropertyType::getId).toList();
        } else if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
            return typeManager.getAllTypes().stream().map(PropertyType::getId).toList();
        }
        return Collections.emptyList();
    }
}
