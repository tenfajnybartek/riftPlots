package pl.riftplots.command.sub;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotMember;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class TrustSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final MessageConfiguration messageConfig;

    public TrustSubCommand(PlotManager plotManager, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "trust";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_TRUST;
    }

    @Override
    public String getUsage() {
        return "plots trust <gracz> [działka]";
    }

    @Override
    public String getDescription() {
        return "Dodaje gracza jako współużytkownika do działki.";
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

        String targetName = args[0];
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getName() == null) {
            player.sendMessage(messageConfig.get("members.player-not-found", Map.of("target", targetName)));
            return true;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(messageConfig.get("members.cannot-trust-self"));
            return true;
        }

        Plot targetPlot = null;
        if (args.length > 1) {
            String plotId = args[1];
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
            AdventureUtil.sendMessage(player, "<red>Podaj identyfikator działki lub stań na jej terenie!</red>");
            return true;
        }

        if (!targetPlot.isOwner(player.getUniqueId()) && !player.hasPermission(PermissionConfiguration.ADMIN_BYPASS)) {
            player.sendMessage(messageConfig.get("plot.not-owner", Map.of("plot_id", targetPlot.getId())));
            return true;
        }

        if (targetPlot.isMember(target.getUniqueId())) {
            player.sendMessage(messageConfig.get("members.already-trusted", Map.of("target", target.getName())));
            return true;
        }

        PlotMember newMember = PlotMember.defaultMember(target.getUniqueId(), target.getName());
        targetPlot.addMember(newMember);
        plotManager.savePlot(targetPlot);

        player.sendMessage(messageConfig.get("members.trust-success", Map.of(
                "target", target.getName(),
                "name", targetPlot.getName(),
                "plot_id", targetPlot.getId()
        )));

        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(messageConfig.get("members.target-trusted-notify", Map.of(
                    "name", targetPlot.getName(),
                    "plot_id", targetPlot.getId(),
                    "owner", player.getName()
            )));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2 && sender instanceof Player player) {
            return plotManager.getPlotsByOwner(player.getUniqueId()).stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
