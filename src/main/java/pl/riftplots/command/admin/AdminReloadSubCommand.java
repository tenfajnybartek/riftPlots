package pl.riftplots.command.admin;

import org.bukkit.command.CommandSender;
import pl.riftplots.RiftPlotsPlugin;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;

import java.util.Collections;
import java.util.List;

public class AdminReloadSubCommand implements SubCommand {
    private final RiftPlotsPlugin plugin;
    private final MessageConfiguration messageConfig;

    public AdminReloadSubCommand(RiftPlotsPlugin plugin, MessageConfiguration messageConfig) {
        this.plugin = plugin;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "reload";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_RELOAD;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-reload.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-reload.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        plugin.reloadPlugin();
        sender.sendMessage(messageConfig.get("general.reload-success"));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
