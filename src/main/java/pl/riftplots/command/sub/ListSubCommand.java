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
        return "plots list";
    }

    @Override
    public String getDescription() {
        return "Wyświetla listę Twoich działek.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (sender instanceof Player player) {
            listGUI.open(player);
            return true;
        }

        AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== LISTA DZIAŁEK MIEJSKICH ===</b></gradient>");
        for (Plot plot : plotManager.getAllPlots()) {
            AdventureUtil.sendMessage(sender, "<yellow>" + plot.getId() + "</yellow> - <gold>" + plot.getName() + "</gold> [" + plot.getStatus() + "] (Wł.: " + (plot.getOwnerName() != null ? plot.getOwnerName() : "Brak") + ")");
        }
        return true;
    }
}
