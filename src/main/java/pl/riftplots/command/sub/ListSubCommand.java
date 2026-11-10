package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.gui.PlotListGUI;
import pl.riftplots.service.payment.TaxCalculator;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.List;

public class ListSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PluginConfiguration config;
    private final TaxCalculator taxCalculator;
    private final PlotListGUI listGUI;

    public ListSubCommand(PlotManager plotManager, PluginConfiguration config, TaxCalculator taxCalculator, PlotListGUI listGUI) {
        this.plotManager = plotManager;
        this.config = config;
        this.taxCalculator = taxCalculator;
        this.listGUI = listGUI;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_LIST;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.list.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.list.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            listGUI.open(player);
            return true;
        }

        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.list.lista-dziaek-miejskich"));
        for (Plot plot : plotManager.getAllPlots()) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.list.w", plot.getId(), plot.getName(), plot.getStatus(), (plot.getOwnerName() != null ? plot.getOwnerName() : pl.riftplots.config.Texts.raw("text.list.brak"))));
        }
        return true;
    }
}
