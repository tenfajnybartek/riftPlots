package pl.riftplots.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotMember;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.plot.PlotManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import pl.riftplots.util.AdventureUtil;

import java.util.Optional;
import java.util.UUID;

public class PlotGUIListener implements Listener {
    private final PlotManager plotManager;
    private final PaymentService paymentService;
    private final PlotListGUI listGUI;
    private final PlotDetailsGUI detailsGUI;
    private final PlotConfirmBuyGUI confirmBuyGUI;
    private final PlotMembersGUI membersGUI;

    public PlotGUIListener(PlotManager plotManager,
                           PaymentService paymentService,
                           PlotListGUI listGUI,
                           PlotDetailsGUI detailsGUI,
                           PlotConfirmBuyGUI confirmBuyGUI,
                           PlotMembersGUI membersGUI) {
        this.plotManager = plotManager;
        this.paymentService = paymentService;
        this.listGUI = listGUI;
        this.detailsGUI = detailsGUI;
        this.confirmBuyGUI = confirmBuyGUI;
        this.membersGUI = membersGUI;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PlotGUIHolder holder)) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (!(event.getWhoClicked() instanceof Player player)
                || !player.hasPermission("riftplots.user") || !player.hasPermission("riftplots.user.gui")) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String type = holder.getGuiType();

        switch (type) {
            case "PLOT_LIST" -> handlePlotListClick(player, event.getSlot(), clicked);
            case "PLOT_AVAILABLE_LIST" -> handleAvailableListClick(player, event.getSlot(), clicked);
            case "PLOT_DETAILS" -> handleDetailsClick(player, holder.getPlotId(), event.getSlot(), clicked);
            case "PLOT_CONFIRM_BUY" -> handleConfirmBuyClick(player, holder.getPlotId(), event.getSlot(), clicked);
            case "PLOT_MEMBERS" -> handleMembersClick(player, holder.getPlotId(), event.getSlot(), clicked);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PlotGUIHolder
                && event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize())) {
            event.setCancelled(true);
        }
    }

    private void handlePlotListClick(Player player, int slot, ItemStack clicked) {
        if (slot == 49 && clicked.getType() == Material.EMERALD) {
            listGUI.openAvailable(player);
            return;
        }

        if (clicked.getType() == Material.GOLD_BLOCK) {
            String plotId = extractPlotIdFromItem(clicked);
            if (plotId != null) {
                plotManager.getPlot(plotId).ifPresent(plot -> detailsGUI.open(player, plot));
            }
        }
    }

    private void handleAvailableListClick(Player player, int slot, ItemStack clicked) {
        if (slot == 49 && clicked.getType() == Material.ARROW) {
            listGUI.open(player);
            return;
        }

        if (clicked.getType() == Material.GRASS_BLOCK) {
            String plotId = extractPlotIdFromItem(clicked);
            if (plotId != null) {
                plotManager.getPlot(plotId).ifPresent(plot -> confirmBuyGUI.open(player, plot));
            }
        }
    }

    private void handleDetailsClick(Player player, String plotId, int slot, ItemStack clicked) {
        Optional<Plot> optPlot = plotManager.getPlot(plotId);
        if (optPlot.isEmpty()) {
            player.closeInventory();
            return;
        }
        Plot plot = optPlot.get();
        if (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("riftplots.admin.bypass")) {
            player.closeInventory();
            return;
        }

        if (slot == 13 && player.hasPermission("riftplots.user.pay")) {
            paymentService.payTax(player, plot, 1);
            detailsGUI.open(player, plot);
        } else if (slot == 15 && player.hasPermission("riftplots.user.members")) {
            membersGUI.open(player, plot);
        } else if (slot == 31 && player.hasPermission("riftplots.user.abandon")) {
            player.closeInventory();
            player.performCommand("plots abandon " + plot.getId());
        } else if (slot == 36) {
            listGUI.open(player);
        }
    }

    private void handleConfirmBuyClick(Player player, String plotId, int slot, ItemStack clicked) {
        Optional<Plot> optPlot = plotManager.getPlot(plotId);
        if (optPlot.isEmpty()) {
            player.closeInventory();
            return;
        }
        Plot plot = optPlot.get();

        if (slot == 11 && clicked.getType() == Material.LIME_WOOL && player.hasPermission("riftplots.user.buy")) {
            player.closeInventory();
            paymentService.buyPlot(player, plot);
        } else if (slot == 15 && clicked.getType() == Material.RED_WOOL) {
            listGUI.openAvailable(player);
        }
    }

    private void handleMembersClick(Player player, String plotId, int slot, ItemStack clicked) {
        Optional<Plot> optPlot = plotManager.getPlot(plotId);
        if (optPlot.isEmpty()) {
            player.closeInventory();
            return;
        }
        Plot plot = optPlot.get();
        if ((!plot.isOwner(player.getUniqueId()) && !player.hasPermission("riftplots.admin.bypass"))
                || !player.hasPermission("riftplots.user.untrust")) return;

        if (slot == 40 && clicked.getType() == Material.ARROW) {
            detailsGUI.open(player, plot);
            return;
        }

        if (clicked.getType() == Material.PLAYER_HEAD) {
            String memberName = extractMemberName(clicked);
            if (memberName != null) {
                UUID toRemove = null;
                for (PlotMember member : plot.getMembers().values()) {
                    if (member.getPlayerName().equalsIgnoreCase(memberName)) {
                        if (member.getRole() == pl.riftplots.domain.plot.PlotMemberRole.TENANT) {
                            AdventureUtil.sendMessage(player, "<red>Aktywny najem należy zakończyć przez /plots admin rent cancel.</red>");
                            return;
                        }
                        toRemove = member.getPlayerUuid();
                        break;
                    }
                }
                if (toRemove != null) {
                    plot.removeMember(toRemove);
                    plotManager.savePlot(plot);
                    AdventureUtil.sendMessage(player, "<green>Usunięto członka: " + memberName + "</green>");
                    membersGUI.open(player, plot);
                }
            }
        }
    }

    private String extractPlotIdFromItem(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            net.kyori.adventure.text.Component comp = item.getItemMeta().displayName();
            if (comp != null) {
                String plain = PlainTextComponentSerializer.plainText().serialize(comp);
                int openParen = plain.lastIndexOf('(');
                int closeParen = plain.lastIndexOf(')');
                if (openParen != -1 && closeParen != -1 && closeParen > openParen) {
                    return plain.substring(openParen + 1, closeParen).trim();
                }
            }
        }
        return null;
    }

    private String extractMemberName(ItemStack item) {
        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
            net.kyori.adventure.text.Component comp = item.getItemMeta().displayName();
            if (comp != null) {
                String plain = PlainTextComponentSerializer.plainText().serialize(comp);
                return plain.replaceAll("[^a-zA-Z0-9_]", "").trim();
            }
        }
        return null;
    }
}
