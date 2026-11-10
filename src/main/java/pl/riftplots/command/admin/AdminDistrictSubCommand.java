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
        return pl.riftplots.config.Texts.raw("text.admin-district.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-district.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.lista-dzielnic-miejskich"));
            for (District d : districtManager.getAllDistricts()) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.ceny-x-podatki-x-max", d.getId(), d.getName(), d.getPriceMultiplier(), d.getTaxMultiplier(), d.getMaxPlotsPerPlayer()));
            }
            return true;
        }

        if ("set".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-district.uzycie-plots-admin-district-set"));
                return true;
            }
            String plotId = args[1].toLowerCase();
            String districtId = args[2].toLowerCase();

            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isEmpty()) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-district.dziaka-nie-istnieje"));
                return true;
            }
            Plot plot = opt.get();

            if ("none".equals(districtId)) {
                plot.setDistrictId(null);
                plotManager.savePlot(plot);
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-district.usunieto-przypisanie-dzielnicy-dla-dziaki", plotId));
                return true;
            }

            Optional<District> optDist = districtManager.getDistrict(districtId);
            if (optDist.isEmpty()) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-district.dzielnica-o-id-nie-istnieje", districtId));
                return true;
            }

            plot.setDistrictId(districtId);
            plotManager.savePlot(plot);
            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-district.przypisano-dziake-do-dzielnicy", plotId, optDist.get().getName()));
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-district.uzycie-plots-admin-district-info"));
                return true;
            }
            String distId = args[1].toLowerCase();
            Optional<District> optDist = districtManager.getDistrict(distId);
            if (optDist.isEmpty()) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-district.dzielnica-nie-istnieje"));
                return true;
            }
            District d = optDist.get();
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.informacje-o-dzielnicy", d.getName()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.id", d.getId()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.opis", d.getDescription()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.mnoznik-cen-x", d.getPriceMultiplier()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.mnoznik-podatku-x", d.getTaxMultiplier()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.dozwolone-typy", d.getAllowedPropertyTypes()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.limit-dziaek-na-gracza", d.getMaxPlotsPerPlayer()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-district.sprzedaz-wynajem", d.isSalesEnabled(), d.isRentalEnabled()));
            return true;
        }

        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-district.uzycie", getUsage()));
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
