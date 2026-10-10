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
        return "plots admin unsign";
    }

    @Override
    public String getDescription() {
        return "Usuwa powiązanie tabliczki, na którą patrzysz.";
    }

    @Override
    public boolean execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messageConfig.get("general.only-player"));
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
            AdventureUtil.sendMessage(player, "<red>Musisz patrzeć bezpośrednio na tabliczkę!</red>");
            return true;
        }

        Optional<PlotSign> optSign = signService.getSignAt(
                targetBlock.getWorld().getName(),
                targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()
        );

        if (optSign.isEmpty()) {
            AdventureUtil.sendMessage(player, "<red>Ta tabliczka nie jest powiązana z żadną działką!</red>");
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
