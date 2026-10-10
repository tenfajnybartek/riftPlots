package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.district.District;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.district.DistrictManager;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminDistrictSubCommand implements SubCommand {
    private final DistrictManager districtManager;
    private final PlotManager plotManager;

    public AdminDistrictSubCommand(DistrictManager districtManager, PlotManager plotManager) {
        this.districtManager = districtManager;
        this.plotManager = plotManager;
    }

    @Override
    public String getName() {
        return "district";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_DISTRICT;
    }

    @Override
    public String getUsage() {
        return "plots admin district <list|set <działka> <dzielnica>|info <dzielnica>>";
    }

    @Override
    public String getDescription() {
        return "Zarządzanie dzielnicami miejskimi i przypisywaniem działek.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== LISTA DZIELNIC MIEJSKICH ===</b></gradient>");
            for (District d : districtManager.getAllDistricts()) {
                AdventureUtil.sendMessage(sender, "<yellow>" + d.getId() + "</yellow> - <gold>" + d.getName() + "</gold> <gray>(Ceny: " + d.getPriceMultiplier() + "x, Podatki: " + d.getTaxMultiplier() + "x, Max: " + d.getMaxPlotsPerPlayer() + ")</gray>");
            }
            return true;
        }

        if ("set".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage("§cUżycie: /plots admin district set <działka> <dzielnica|none>");
                return true;
            }
            String plotId = args[1].toLowerCase();
            String districtId = args[2].toLowerCase();

            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isEmpty()) {
                sender.sendMessage("§cDziałka nie istnieje.");
                return true;
            }
            Plot plot = opt.get();

            if ("none".equals(districtId)) {
                plot.setDistrictId(null);
                plotManager.savePlot(plot);
                sender.sendMessage("§aUsunięto przypisanie dzielnicy dla działki " + plotId + ".");
                return true;
            }

            Optional<District> optDist = districtManager.getDistrict(districtId);
            if (optDist.isEmpty()) {
                sender.sendMessage("§cDzielnica o id " + districtId + " nie istnieje!");
                return true;
            }

            plot.setDistrictId(districtId);
            plotManager.savePlot(plot);
            sender.sendMessage("§aPrzypisano działkę " + plotId + " do dzielnicy " + optDist.get().getName() + ".");
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage("§cUżycie: /plots admin district info <dzielnica>");
                return true;
            }
            String distId = args[1].toLowerCase();
            Optional<District> optDist = districtManager.getDistrict(distId);
            if (optDist.isEmpty()) {
                sender.sendMessage("§cDzielnica nie istnieje.");
                return true;
            }
            District d = optDist.get();
            AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== INFORMACJE O DZIELNICY: " + d.getName() + " ===</b></gradient>");
            AdventureUtil.sendMessage(sender, "<gray>ID: </gray><white>" + d.getId() + "</white>");
            AdventureUtil.sendMessage(sender, "<gray>Opis: </gray><yellow>" + d.getDescription() + "</yellow>");
            AdventureUtil.sendMessage(sender, "<gray>Mnożnik cen: </gray><gold>" + d.getPriceMultiplier() + "x</gold>");
            AdventureUtil.sendMessage(sender, "<gray>Mnożnik podatku: </gray><gold>" + d.getTaxMultiplier() + "x</gold>");
            AdventureUtil.sendMessage(sender, "<gray>Dozwolone typy: </gray><aqua>" + d.getAllowedPropertyTypes() + "</aqua>");
            AdventureUtil.sendMessage(sender, "<gray>Limit działek na gracza: </gray><white>" + d.getMaxPlotsPerPlayer() + "</white>");
            AdventureUtil.sendMessage(sender, "<gray>Sprzedaż: </gray><yellow>" + d.isSalesEnabled() + "</yellow> | <gray>Wynajem: </gray><yellow>" + d.isRentalEnabled() + "</yellow>");
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
            List<String> list = new ArrayList<>(districtManager.getAllDistricts().stream().map(District::getId).toList());
            list.add("none");
            return list;
        } else if (args.length == 2 && "info".equalsIgnoreCase(args[0])) {
            return districtManager.getAllDistricts().stream().map(District::getId).toList();
        }
        return Collections.emptyList();
    }
}
