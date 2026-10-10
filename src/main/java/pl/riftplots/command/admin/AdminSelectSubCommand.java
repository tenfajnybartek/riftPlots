package pl.riftplots.command.admin;

import com.sk89q.worldedit.IncompleteRegionException;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.regions.CuboidRegion;
import com.sk89q.worldedit.session.SessionManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotBounds;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminSelectSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final MessageConfiguration messageConfig;

    public AdminSelectSubCommand(PlotManager plotManager, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "select";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_SELECT;
    }

    @Override
    public String getUsage() {
        return "plots admin select <id>";
    }

    @Override
    public String getDescription() {
        return "Ustawia zaznaczenie WorldEdit na obszar wybranej działki.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(messageConfig.get("general.invalid-arguments", Map.of("usage", getUsage())));
            return true;
        }

        String id = args[0].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(id);
        if (opt.isEmpty()) {
            player.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", id)));
            return true;
        }

        Plot plot = opt.get();
        PlotBounds bounds = plot.getBounds();

        BlockVector3 min = BlockVector3.at(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
        BlockVector3 max = BlockVector3.at(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());

        com.sk89q.worldedit.world.World weWorld = BukkitAdapter.adapt(player.getWorld());
        com.sk89q.worldedit.regions.selector.CuboidRegionSelector selector =
                new com.sk89q.worldedit.regions.selector.CuboidRegionSelector(weWorld, min, max);
        SessionManager sessionManager = WorldEdit.getInstance().getSessionManager();
        sessionManager.get(BukkitAdapter.adapt(player)).setRegionSelector(weWorld, selector);

        AdventureUtil.sendMessage(player, "<green>Ustawiono zaznaczenie WorldEdit na granice działki <yellow>" + id + "</yellow> (" + bounds.getHorizontalArea() + " m²)!</green>");
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
