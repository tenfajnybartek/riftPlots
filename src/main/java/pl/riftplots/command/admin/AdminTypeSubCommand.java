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
        return pl.riftplots.config.Texts.raw("text.admin-type.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-type.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (args.length >= 2 && args[0].equalsIgnoreCase("apply-defaults")) {
            Plot plot = plotManager.getPlot(args[1]).orElse(null);
            if (plot == null) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.dziaka-nie-istnieje"));
                return true;
            }
            var type = typeManager.getType(plot.getPropertyType());
            if (args.length != 3 || !args[2].equalsIgnoreCase("confirm")) {
                var preview = plot.copy();
                preview.applyTypeDefaults(type);
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.cena-bazowa-mnoznik-cen-dzielnicy-cena-z", plot.getPrice(), type.getDefaultPrice(), plotManager.getDistrictPriceMultiplier(plot), plotManager.getPurchasePrice(preview), plot.getBaseTax(), type.getDefaultBaseTax(), plot.getTaxMode(), type.getDefaultTaxMode(), plot.getTaxMultiplier(), type.getDefaultTaxMultiplier(), plot.getBillingPeriodDays(), type.getDefaultBillingPeriodDays(), type.getRefundPercent(), type.getDefaultFlags(), plot.getId()));
                return true;
            }
            try {
                plotManager.applyTypeDefaults(plot);
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.zastosowano-domyslne-parametry-typu-aktywne-umowy-nie"));
            } catch (IllegalArgumentException e) {
                sender.sendMessage(e.getMessage());
            }
            return true;
        }
        if (args.length == 0 || "list".equalsIgnoreCase(args[0])) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.typy-nieruchomosci"));
            for (PropertyType t : typeManager.getAllTypes()) {
                AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.cena-bazowa-podatek-wynajem", t.getId(), t.getDisplayName(), t.getDefaultPrice(), t.getDefaultBaseTax(), t.isRentalAllowed()));
            }
            return true;
        }

        if ("set".equalsIgnoreCase(args[0])) {
            if (args.length < 3) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.uzycie-plots-admin-type-set"));
                return true;
            }
            String plotId = args[1].toLowerCase();
            String typeId = args[2].toUpperCase();
            if (!typeManager.exists(typeId)) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.nieznany-typ-nieruchomosci", typeId));
                return true;
            }

            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isEmpty()) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.dziaka-nie-istnieje-da45d35"));
                return true;
            }

            PropertyType propType = typeManager.getType(typeId);
            Plot plot = opt.get();
            plot.setPropertyType(propType.getId());
            plotManager.savePlot(plot);

            sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.ustawiono-typ-dziaki-na", plotId, propType.getDisplayName(), propType.getId()));
            return true;
        }

        if ("info".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.uzycie-plots-admin-type-info"));
                return true;
            }
            String typeId = args[1].toUpperCase();
            if (!typeManager.exists(typeId)) {
                sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.nieznany-typ-nieruchomosci", typeId));
                return true;
            }
            PropertyType t = typeManager.getType(typeId);
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.informacje-o-typie", t.getDisplayName()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.id", t.getId()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.opis", t.getDescription()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.domyslna-cena", t.getDefaultPrice()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.domyslny-podatek-tryb", t.getDefaultBaseTax(), t.getDefaultTaxMode()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.limit-czonkow", t.getMaxMembers()));
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-type.dozwolony-wynajem-dozwolona-sprzedaz", t.isRentalAllowed(), t.isSalesAllowed()));
            return true;
        }

        sender.sendMessage(pl.riftplots.config.Texts.component("text.admin-type.uzycie", getUsage()));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return List.of("list", "set", "info", "apply-defaults");
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
