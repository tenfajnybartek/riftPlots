package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AdminListSubCommand implements SubCommand {
    private final PlotManager plotManager;

    public AdminListSubCommand(PlotManager plotManager) {
        this.plotManager = plotManager;
    }

    @Override
    public String getName() {
        return "list";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_LIST;
    }

    @Override
    public String getUsage() {
        return "plots admin list";
    }

    @Override
    public String getDescription() {
        return "Wyświetla listę wszystkich działek w systemie.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Collection<Plot> allPlots = plotManager.getAllPlots();
        AdventureUtil.sendMessage(sender, "<gradient:#ff9900:#ff5500><b>=== WSZYSTKIE DZIAŁKI MIEJSKIE (" + allPlots.size() + ") ===</b></gradient>");

        if (allPlots.isEmpty()) {
            AdventureUtil.sendMessage(sender, "<gray>Brak utworzonych działek.</gray>");
            return true;
        }

        for (Plot plot : allPlots) {
            String statusColor = switch (plot.getStatus()) {
                case AVAILABLE -> "<green>";
                case OWNED -> "<dark_green>";
                case PAYMENT_DUE -> "<gold>";
                case GRACE_PERIOD -> "<red>";
                case RESETTING -> "<gray>";
                case DISABLED -> "<dark_red>";
            };
            AdventureUtil.sendMessage(sender, "<yellow>" + plot.getId() + "</yellow> - <white>" + plot.getName() + "</white> " + statusColor + "[" + plot.getStatus() + "]</color> <gray>(Wł.: " + (plot.getOwnerName() != null ? plot.getOwnerName() : "Brak") + ", Pow: " + plot.getBounds().getHorizontalArea() + "m²)</gray>");
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
