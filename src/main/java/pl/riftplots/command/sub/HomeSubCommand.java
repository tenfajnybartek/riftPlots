package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.teleport.TeleportService;

import java.util.*;

public class HomeSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final TeleportService teleportService;

    public HomeSubCommand(PlotManager plotManager, TeleportService teleportService) {
        this.plotManager = plotManager;
        this.teleportService = teleportService;
    }

    @Override
    public String getName() {
        return "home";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_HOME;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.home.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.home.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(pl.riftplots.config.Texts.component("text.home.ta-komenda-jest-dostepna-tylko-dla-graczy"));
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
            List<Plot> owned = plotManager.getPlotsByOwner(player.getUniqueId());
            if (!owned.isEmpty()) {
                targetPlot = owned.get(0);
            }
        }

        if (targetPlot == null) {
            player.sendMessage(pl.riftplots.config.Texts.component("text.home.podaj-identyfikator-dziaki-lub-zakup-dziake-przed"));
            return true;
        }

        teleportService.teleportHome(player, targetPlot);
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
