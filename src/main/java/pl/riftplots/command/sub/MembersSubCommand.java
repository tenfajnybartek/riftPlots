package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotMember;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.util.*;

public class MembersSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final MessageConfiguration messageConfig;

    public MembersSubCommand(PlotManager plotManager, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "members";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_MEMBERS;
    }

    @Override
    public String getUsage() {
        return "plots members [działka]";
    }

    @Override
    public String getDescription() {
        return "Wyświetla listę współużytkowników działki.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        Plot targetPlot = null;

        if (args.length > 0) {
            String plotId = args[0];
            Optional<Plot> opt = plotManager.getPlot(plotId);
            if (opt.isPresent()) {
                targetPlot = opt.get();
            }
        } else if (sender instanceof Player player) {
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
            AdventureUtil.sendMessage(sender, "<red>Podaj identyfikator działki lub stań na jej terenie!</red>");
            return true;
        }

        AdventureUtil.sendMessage(sender, "<gradient:#00c6ff:#0072ff><b>=== WSPÓŁUŻYTKOWNICY DZIAŁKI " + targetPlot.getId().toUpperCase() + " ===</b></gradient>");
        AdventureUtil.sendMessage(sender, "<gray>Właściciel: </gray><gold>" + (targetPlot.getOwnerName() != null ? targetPlot.getOwnerName() : "Brak") + "</gold>");

        Map<UUID, PlotMember> members = targetPlot.getMembers();
        if (members.isEmpty()) {
            AdventureUtil.sendMessage(sender, "<gray>Brak dodanych współużytkowników.</gray>");
            return true;
        }

        for (PlotMember member : members.values()) {
            AdventureUtil.sendMessage(sender, "<yellow>• " + member.getPlayerName() + "</yellow> <gray>(Rola: <gold>" + member.getRole() + "</gold>, Dodano: <white>" + TimeUtil.formatInstant(member.getAddedAt()) + "</white>, Uprawnienia: <aqua>" + member.getPermissions().size() + "</aqua>)</gray>");
        }

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
