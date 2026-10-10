package pl.riftplots.command.admin;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import pl.riftplots.command.SubCommand;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.domain.sign.PlotSignType;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.util.AdventureUtil;

import java.util.*;

public class AdminSignSubCommand implements SubCommand {
    private final PlotManager plotManager;
    private final PlotSignService signService;
    private final MessageConfiguration messageConfig;

    public AdminSignSubCommand(PlotManager plotManager, PlotSignService signService, MessageConfiguration messageConfig) {
        this.plotManager = plotManager;
        this.signService = signService;
        this.messageConfig = messageConfig;
    }

    @Override
    public String getName() {
        return "sign";
    }

    @Override
    public String getPermission() {
        return PermissionConfiguration.ADMIN_SIGN;
    }

    @Override
    public String getUsage() {
        return "plots admin sign <id> [FOR_SALE|STATUS|RENEW]";
    }

    @Override
    public String getDescription() {
        return "Wiąże tabliczkę, na którą patrzysz, z wybraną działką.";
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

        String id = args[0].toLowerCase();
        Optional<Plot> opt = plotManager.getPlot(id);
        if (opt.isEmpty()) {
            player.sendMessage(messageConfig.get("plot.not-found", Map.of("plot_id", id)));
            return true;
        }

        Block targetBlock = player.getTargetBlockExact(5);
        if (targetBlock == null || !(targetBlock.getState() instanceof Sign)) {
            AdventureUtil.sendMessage(player, "<red>Musisz patrzeć bezpośrednio na tabliczkę (w odległości do 5 bloków)!</red>");
            return true;
        }

        PlotSignType type = PlotSignType.FOR_SALE;
        if (args.length > 1) {
            type = PlotSignType.parse(args[1]);
        }

        PlotSign registered = signService.registerSign(
                id, type,
                targetBlock.getWorld().getName(),
                targetBlock.getX(), targetBlock.getY(), targetBlock.getZ()
        );

        player.sendMessage(messageConfig.get("admin.sign-linked", Map.of(
                "plot_id", id,
                "type", registered.getType().name()
        )));

        return true;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return plotManager.getAllPlots().stream()
                    .map(Plot::getId)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .toList();
        } else if (args.length == 2) {
            return Arrays.stream(PlotSignType.values()).map(Enum::name).toList();
        }
        return Collections.emptyList();
    }
}
