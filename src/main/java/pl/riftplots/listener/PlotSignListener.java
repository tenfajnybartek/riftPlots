package pl.riftplots.listener;

import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import pl.riftplots.config.PermissionConfiguration;
import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.service.sign.PlotSignService;
import pl.riftplots.util.AdventureUtil;

import java.util.Optional;

public class PlotSignListener implements Listener {
    private final PlotSignService signService;

    public PlotSignListener(PlotSignService signService) {
        this.signService = signService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign)) {
            return;
        }

        Optional<PlotSign> optSign = signService.getSignAt(
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()
        );

        if (optSign.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();
        if (!player.hasPermission(PermissionConfiguration.ADMIN_SIGN) && !player.hasPermission(PermissionConfiguration.ADMIN_BYPASS)) {
            event.setCancelled(true);
            AdventureUtil.sendMessage(player, "<red>Nie masz uprawnień do niszczenia tabliczek działek!</red>");
            return;
        }

        signService.removeSign(optSign.get().getId());
        AdventureUtil.sendMessage(player, "<green>Pomyślnie usunięto powiązanie tabliczki z działką.</green>");
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.LEFT_CLICK_BLOCK) {
            return;
        }

        Block block = event.getClickedBlock();
        if (block == null || !(block.getState() instanceof Sign)) {
            return;
        }

        Optional<PlotSign> optSign = signService.getSignAt(
                block.getWorld().getName(),
                block.getX(),
                block.getY(),
                block.getZ()
        );

        if (optSign.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        boolean isRightClick = event.getAction() == Action.RIGHT_CLICK_BLOCK;
        signService.handleSignClick(event.getPlayer(), optSign.get(), isRightClick);
    }
}
