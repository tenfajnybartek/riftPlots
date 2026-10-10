package pl.riftplots.listener;

import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.PlotPermission;
import pl.riftplots.service.protection.PlotProtectionService;

public class PlotProtectionListener implements Listener {
    private final PluginConfiguration config;
    private final MessageConfiguration messageConfig;
    private final PlotProtectionService protectionService;

    public PlotProtectionListener(PluginConfiguration config,
                                  MessageConfiguration messageConfig,
                                  PlotProtectionService protectionService) {
        this.config = config;
        this.messageConfig = messageConfig;
        this.protectionService = protectionService;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!protectionService.canInteract(player, block.getLocation(), PlotPermission.DESTROY)) {
            event.setCancelled(true);
            player.sendMessage(messageConfig.get("protection.action-denied"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!protectionService.canInteract(player, block.getLocation(), PlotPermission.BUILD)) {
            event.setCancelled(true);
            player.sendMessage(messageConfig.get("protection.action-denied"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.PHYSICAL) {
            return;
        }
        Block block = event.getClickedBlock();
        if (block == null) {
            return;
        }

        Player player = event.getPlayer();
        Material mat = block.getType();

        PlotPermission required = PlotPermission.INTERACT;
        if (isContainer(mat)) {
            required = PlotPermission.CONTAINERS;
        } else if (isDoor(mat)) {
            required = PlotPermission.DOORS;
        }

        if (!protectionService.canInteract(player, block.getLocation(), required)) {
            event.setCancelled(true);
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                if (required == PlotPermission.CONTAINERS) {
                    player.sendMessage(messageConfig.get("protection.container-denied"));
                } else {
                    player.sendMessage(messageConfig.get("protection.interact-denied"));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArmorStandManipulate(PlayerArmorStandManipulateEvent event) {
        if (!config.isProtectArmorStands()) {
            return;
        }
        Player player = event.getPlayer();
        ArmorStand armorStand = event.getRightClicked();
        if (!protectionService.canInteract(player, armorStand.getLocation(), PlotPermission.CONTAINERS)) {
            event.setCancelled(true);
            player.sendMessage(messageConfig.get("protection.interact-denied"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        Entity target = event.getRightClicked();
        if (target instanceof ItemFrame && config.isProtectItemFrames()) {
            Player player = event.getPlayer();
            if (!protectionService.canInteract(player, target.getLocation(), PlotPermission.INTERACT)) {
                event.setCancelled(true);
                player.sendMessage(messageConfig.get("protection.interact-denied"));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        Entity target = event.getEntity();
        if ((target instanceof ItemFrame && config.isProtectItemFrames())
                || (target instanceof ArmorStand && config.isProtectArmorStands())) {
            if (event.getDamager() instanceof Player player) {
                if (!protectionService.canInteract(player, target.getLocation(), PlotPermission.DESTROY)) {
                    event.setCancelled(true);
                    player.sendMessage(messageConfig.get("protection.action-denied"));
                }
            } else {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        if (!config.isProtectItemFrames()) {
            return;
        }
        Entity remover = event.getRemover();
        if (remover instanceof Player player) {
            if (!protectionService.canInteract(player, event.getEntity().getLocation(), PlotPermission.DESTROY)) {
                event.setCancelled(true);
                player.sendMessage(messageConfig.get("protection.action-denied"));
            }
        } else {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleEnter(VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            if (!protectionService.canInteract(player, event.getVehicle().getLocation(), PlotPermission.VEHICLES)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onVehicleDamage(VehicleDamageEvent event) {
        if (event.getAttacker() instanceof Player player) {
            if (!protectionService.canInteract(player, event.getVehicle().getLocation(), PlotPermission.DESTROY)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(org.bukkit.event.player.PlayerBucketEmptyEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!protectionService.canInteract(player, block.getLocation(), PlotPermission.BUILD)) {
            event.setCancelled(true);
            player.sendMessage(messageConfig.get("protection.action-denied"));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketFill(org.bukkit.event.player.PlayerBucketFillEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (!protectionService.canInteract(player, block.getLocation(), PlotPermission.DESTROY)) {
            event.setCancelled(true);
            player.sendMessage(messageConfig.get("protection.action-denied"));
        }
    }

    private boolean isContainer(Material material) {
        if (Tag.SHULKER_BOXES.isTagged(material)) {
            return true;
        }
        return switch (material) {
            case CHEST, TRAPPED_CHEST, BARREL, HOPPER, DISPENSER, DROPPER,
                 FURNACE, BLAST_FURNACE, SMOKER, BREWING_STAND, CHISELED_BOOKSHELF, CRAFTER -> true;
            default -> false;
        };
    }

    private boolean isDoor(Material material) {
        return Tag.DOORS.isTagged(material)
                || Tag.TRAPDOORS.isTagged(material)
                || Tag.FENCE_GATES.isTagged(material)
                || Tag.BUTTONS.isTagged(material)
                || material == Material.LEVER;
    }
}
