package pl.riftplots.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import pl.riftplots.config.PermissionConfiguration;

import java.util.*;

public class PlotTabCompleter implements TabCompleter {
    private final PlotCommand plotCommand;

    public PlotTabCompleter(PlotCommand plotCommand) {
        this.plotCommand = plotCommand;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission(PermissionConfiguration.USER_BASE)) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> list = new ArrayList<>(plotCommand.getSubCommands().keySet());
            if (sender.hasPermission(PermissionConfiguration.ADMIN_BASE)) {
                list.add("admin");
            }
            return list.stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase()))
                    .toList();
        }

        if (args.length > 1) {
            String subName = args[0].toLowerCase();
            if ("admin".equals(subName)) {
                String[] adminArgs = Arrays.copyOfRange(args, 1, args.length);
                return plotCommand.getAdminDispatcher().tabComplete(sender, adminArgs);
            }

            SubCommand sub = plotCommand.getSubCommands().get(subName);
            if (sub != null) {
                String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
                return sub.tabComplete(sender, subArgs);
            }
        }

        return Collections.emptyList();
    }
}
