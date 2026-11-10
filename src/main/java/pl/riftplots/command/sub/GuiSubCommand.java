package pl.riftplots.command.sub;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.gui.PlotListGUI;

public class GuiSubCommand implements SubCommand {
    private final PlotListGUI listGUI;
    private final MessageConfiguration messageConfig;

    public GuiSubCommand(PlotListGUI listGUI, MessageConfiguration messageConfig) {
        this.listGUI = listGUI;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "gui";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.USER_GUI;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.gui.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.gui.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }
        listGUI.open(player);
        return true;
    }
}
