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
        return pl.riftplots.config.Texts.raw("text.admin-list.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-list.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Collection<Plot> allPlots = plotManager.getAllPlots();
        AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-list.wszystkie-dziaki-miejskie", allPlots.size()));

        if (allPlots.isEmpty()) {
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-list.brak-utworzonych-dziaek"));
            return true;
        }

        for (Plot plot : allPlots) {
            String statusColor = switch (plot.getStatus()) {
                case AVAILABLE -> pl.riftplots.config.Texts.raw("text.admin-list.line");
                case OWNED -> pl.riftplots.config.Texts.raw("text.admin-list.line-27cabc2");
                case PAYMENT_DUE -> pl.riftplots.config.Texts.raw("text.admin-list.line-e647b77");
                case GRACE_PERIOD -> pl.riftplots.config.Texts.raw("text.admin-list.line-8b75ec2");
                case RESETTING -> pl.riftplots.config.Texts.raw("text.admin-list.line-9e60a98");
                case DISABLED -> pl.riftplots.config.Texts.raw("text.admin-list.line-e540b91");
            };
            AdventureUtil.sendMessage(sender, pl.riftplots.config.Texts.markup("text.admin-list.w-pow-m2", plot.getId(), plot.getName(), statusColor, plot.getStatus(), (plot.getOwnerName() != null ? plot.getOwnerName() : pl.riftplots.config.Texts.raw("text.admin-list.brak")), plot.getBounds().getHorizontalArea()));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
