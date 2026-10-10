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
        return "plots sethome [działka]";
    }

    @Override
    public String getDescription() {
        return "Ustawia punkt domowy działki w miejscu, w którym stoisz.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cTa komenda jest dostępna tylko dla graczy.");
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
            player.sendMessage("§cPodaj identyfikator działki lub stań na jej terenie!");
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
