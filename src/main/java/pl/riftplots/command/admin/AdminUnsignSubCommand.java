package pl.riftplots.command.admin;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminUnsignSubCommand implements SubCommand {
    private final PlotSignService signService;
    private final MessageConfiguration messageConfig;

    public AdminUnsignSubCommand(PlotSignService signService, MessageConfiguration messageConfig) {
        this.signService = signService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "unsign";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_UNSIGN;
    }

    @Override
    public String getUsage() {
        return pl.riftplots.config.Texts.raw("text.admin-unsign.usage");
    }

    @Override
    public String getDescription() {
        return pl.riftplots.config.Texts.raw("text.admin-unsign.description");
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(pl.riftplots.config.Settings.integer("interaction.sign-target-distance"));
        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
            AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.admin-unsign.musisz-patrzec-bezposrednio-na-tabliczke"));
            return true;
        }

        Optional<PlotSign> optSign = signService.getSignAt(
                targetBlock.getWorld().getName(),
                targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()
        );

        if (optSign.isEmpty()) {
            AdventureUtil.sendMessage(player, pl.riftplots.config.Texts.markup("text.admin-unsign.ta-tabliczka-nie-jest-powiazana-z-zadna"));
            return true;
        }

        PlotSign sign = optSign.get();
        signService.removeSign(sign.getId());

        player.sendMessage(messageConfig.get("admin.sign-unlinked", Map.of("plot_id", sign.getPlotId())));
        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
