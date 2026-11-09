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

public class UntrustSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final MessageConfiguration messageConfig;

    public UntrustSubCommand(PlotManager plotManager, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "untrust";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_UNTRUST;
    }

    @Override
    public String getUsage() {
        return "plots untrust <gracz> [działka]";
    }

    @Override
    public String getDescription() {
        return "Usuwa współużytkownika z działki.";
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

        UUID memberUuid = null;
        for (PlotMember member : targetPlot.getMembers().values()) {
            if (member.getPlayerName().equalsIgnoreCase(targetName)) {
                memberUuid = member.getPlayerUuid();
                break;
            }
        }

        if (memberUuid == null) {
            OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
            if (off.getUniqueId() != null && targetPlot.isMember(off.getUniqueId())) {
                memberUuid = off.getUniqueId();
            }
        }

        if (memberUuid == null) {
            player.sendMessage(messageConfig.get("members.not-trusted", Map.of("target", targetName)));
            return true;
        }

        if (targetPlot.getMember(memberUuid).getRole() == pl.riftplots.domain.plot.PlotMemberRole.TENANT) {
            AdventureUtil.sendMessage(player, "<red>Aktywny najem należy zakończyć przez /plots admin rent cancel.</red>");
            return true;
        }
        targetPlot.removeMember(memberUuid);
        plotManager.savePlot(targetPlot);

        player.sendMessage(messageConfig.get("members.untrust-success", Map.of(
                "target", targetName,
                "name", targetPlot.getName(),
                "plot_id", targetPlot.getId()
        )));

        Player targetPlayer = Bukkit.getPlayer(memberUuid);
        if (targetPlayer != null && targetPlayer.isOnline()) {
            targetPlayer.sendMessage(messageConfig.get("members.target-untrusted-notify", Map.of(
                    "name", targetPlot.getName(),
                    "plot_id", targetPlot.getId()
            )));
        }

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            List<String> list = new ArrayList<>();
            for (Plot plot : plotManager.getPlotsByOwner(player.getUniqueId())) {
                for (PlotMember member : plot.getMembers().values()) {
                    list.add(member.getPlayerName());
                }
            }
            return list.stream().filter(n -> n.toLowerCase().startsWith(args[0].toLowerCase())).distinct().toList();
        } else if (args.length == 2 && sender instanceof Player player) {
            return plotManager.getPlotsByOwner(player.getUniqueId()).stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[1].toLowerCase()))
                    .toList();
        }
        return Collections.emptyList();
    }
}
