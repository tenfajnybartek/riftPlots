package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.teleport.TeleportService;

import java.util.*;

public class SetHomeSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final TeleportService teleportService;

    public SetHomeSubCommand(PlotManager plotManager, TeleportService teleportService) {
        this.plotManager = plotManager;
        this.teleportService = teleportService;
    }

    @Override
    public String getName() {
        return "sethome";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_SETHOME;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.set-home.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.set-home.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.set-home.ta-komenda-jest-dostepna-tylko-dla-graczy"));
            return true;
        }

        Plot targetPlot = null;
        if (args.length > 0) {
            String plotId = args[0];
            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isPresent()) {
                targetPlot = opt.get();
            }
        } else {
            Optional<Plot> opt = plotManager.getPlotAt(
                    player.getWorld().getName(),
                    player.getLocation().getBlockX(),
                    player.getLocation().getBlockY(),
                    player.getLocation().getBlockZ()
            );
            if (opt.isPresent()) {
                targetPlot = opt.get();
            }
        }

        if (targetPlot == null) {
            player.sendMessage(pl.riftplots.config.Texts.component("text.set-home.podaj-identyfikator-dziaki-lub-stan-na-jej"));
            return true;
        }

        teleportService.setHome(player, targetPlot);
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            return plotManager.getPlotsByOwner(player.getUniqueId()).stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
