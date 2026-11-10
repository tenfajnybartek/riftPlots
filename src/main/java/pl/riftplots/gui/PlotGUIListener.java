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
    private pl.riftplots.service.rental.RentalService rentals;
    private pl.riftplots.service.auction.AuctionService auctions;
    private pl.riftplots.service.audit.AuditService audit;

    public void setMarketServices(pl.riftplots.service.rental.RentalService rentals,
                                  pl.riftplots.service.auction.AuctionService auctions, pl.riftplots.service.audit.AuditService audit) {
        this.rentals = rentals;
        this.auctions = auctions;
        this.audit = audit;
    }

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
        if (!(event.getInventory().getHolder() instanceof PlotGUIHolder)) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (!(event.getWhoClicked() instanceof Player player)) return;
        var previous = plotManager.enterActor(player);
        try {
            handleInventoryClick(event);
        } catch (RuntimeException e) {
            java.util.logging.Logger.getLogger("riftPlots").log(java.util.logging.Level.SEVERE, "Nie udało się zakończyć operacji w panelu", e);
            player.closeInventory();
            player.sendMessage("Nie udało się zakończyć operacji. Administracja może sprawdzić jej status.");
        } finally {
            plotManager.restoreActor(previous);
        }
    }

    private void handleInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PlotGUIHolder holder)) {
            return;
        }

        if (!(event.getWhoClicked() instanceof Player player)
                || !player.hasPermission("riftplots.user") || !player.hasPermission("riftplots.user.gui")) return;
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        String type = holder.getGuiType();
        if (holder.getPlotId() != null && (plotManager.isResetPending(holder.getPlotId()) || plotManager.financiallyBlocked(holder.getPlotId(), null))) {
            player.closeInventory();
            player.sendMessage("Nieruchomość jest w trakcie operacji lub wymaga uzgodnienia przez administrację.");
            return;
        }

        if (type.equals("PLOT_MARKET")) {
            handleMarket(player, holder, event.getSlot(), clicked);
            return;
        }
        if (type.equals("PLOT_CONFIRM_RENT") || type.equals("PLOT_AUCTION")) {
            handleMarketConfirmation(player, holder, event.getSlot(), clicked);
            return;
        }
        if (type.equals("PLOT_PERMISSIONS")) {
            handlePermissions(player, holder, event.getSlot(), clicked);
            return;
        }
        if (type.equals("PLOT_MEMBERS")) {
            handleMembers(player, holder, event.getSlot(), clicked, event.isRightClick());
            return;
        }

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
            String quote = GuiData.get(clicked, "quote");
            var price = java.util.Optional.ofNullable(plotManager.getPurchasePrice(plot)).orElse(plot.getPrice());
            if (quote == null || new java.math.BigDecimal(quote).compareTo(price) != 0) {
                confirmBuyGUI.open(player, plot);
                return;
            }
            player.closeInventory();
            paymentService.buyPlot(player, plot);
        } else if (slot == 15 && clicked.getType() == Material.RED_WOOL) {
            listGUI.openAvailable(player);
        } else if (slot == 22 && player.hasPermission("riftplots.border")) {
            player.closeInventory();
            player.performCommand("plots border " + plot.getId());
        }
    }

    private void handleMarket(Player player, PlotGUIHolder holder, int slot, ItemStack clicked) {
        MarketView view = holder.getMarketView();
        if (view == null) return;
        if (slot >= 45) {
            listGUI.market().navigate(player, view, slot);
            return;
        }
        String id = GuiData.get(clicked, "plot");
        Plot plot = plotManager.getPlot(id).orElse(null);
        if (plot == null) {
            listGUI.market().open(player, view);
            return;
        }
        switch (view.tab()) {
            case OWNED -> {
                if (plot.isOwner(player.getUniqueId()) || player.hasPermission("riftplots.admin.bypass"))
                    detailsGUI.open(player, plot);
                else if (rentals != null) player.performCommand("plots rent info " + plot.getId());
            }
            case SALE -> {
                if (plot.isAvailableForPurchase() && player.hasPermission("riftplots.user.buy"))
                    confirmBuyGUI.open(player, plot);
            }
            case RENT -> {
                if (player.hasPermission("riftplots.rent")) listGUI.market().confirmRental(player, plot, false);
            }
            case AUCTION -> {
                if (player.hasPermission("riftplots.auction")) listGUI.market().auction(player, plot);
            }
        }
    }

    private void handleMarketConfirmation(Player player, PlotGUIHolder holder, int slot, ItemStack clicked) {
        Plot plot = plotManager.getPlot(holder.getPlotId()).orElse(null);
        if (plot == null) {
            player.closeInventory();
            return;
        }
        if (holder.getGuiType().equals("PLOT_CONFIRM_RENT")) {
            if (slot == 15) {
                listGUI.market().open(player, MarketView.start(MarketView.Tab.RENT));
                return;
            }
            if (slot == 13) {
                listGUI.market().confirmRental(player, plot, !holder.isAutoRenew());
                return;
            }
            if (slot == 11 && rentals != null && player.hasPermission("riftplots.rent")) {
                String terms = plot.getRentalPrice() + ":" + plot.getRentalDeposit() + ":" + plot.getRentalPeriodUnit();
                if (!terms.equals(GuiData.get(clicked, "terms"))) {
                    listGUI.market().confirmRental(player, plot, holder.isAutoRenew());
                    return;
                }
                player.closeInventory();
                rentals.rentPlot(player, plot, 1, holder.isAutoRenew());
            }
        } else {
            if (slot == 22) {
                listGUI.market().open(player, MarketView.start(MarketView.Tab.AUCTION));
                return;
            }
            if (slot == 13) {
                player.closeInventory();
                player.performCommand("plots auction history " + plot.getId());
                return;
            }
            String quote = GuiData.get(clicked, "quote");
            if ((slot == 11 || slot == 15) && quote != null && auctions != null && player.hasPermission("riftplots.auction")) {
                var currentAuction = auctions.getAuction(plot.getId()).orElse(null);
                if (currentAuction == null || !currentAuction.getId().equals(GuiData.get(clicked, "auction"))) {
                    listGUI.market().auction(player, plot);
                    return;
                }
                player.closeInventory();
                auctions.placeBid(player, plot.getId(), new java.math.BigDecimal(quote));
            }
        }
    }

    private void handleMembers(Player player, PlotGUIHolder holder, int slot, ItemStack clicked, boolean rightClick) {
        Plot plot = plotManager.getPlot(holder.getPlotId()).orElse(null);
        if (plot == null || (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("riftplots.admin.bypass"))) {
            player.closeInventory();
            return;
        }
        if (slot == 40) {
            detailsGUI.open(player, plot);
            return;
        }
        if (slot == 36 || slot == 44) {
            membersGUI.open(player, plot, holder.getPage() + (slot == 36 ? -1 : 1));
            return;
        }
        String memberId = GuiData.get(clicked, "member");
        if (memberId == null) return;
        PlotMember member = plot.getMember(UUID.fromString(memberId));
        if (member == null) {
            membersGUI.open(player, plot, holder.getPage());
            return;
        }
        if (member.getRole() == pl.riftplots.domain.plot.PlotMemberRole.TENANT) {
            player.sendMessage("Uprawnienia najemcy wynikają z umowy.");
            return;
        }
        if (rightClick && player.hasPermission("riftplots.user.untrust")) {
            plot.removeMember(member.getPlayerUuid());
            plotManager.savePlot(plot);
            if (audit != null)
                audit.log(plot.getId(), player.getUniqueId(), player.getName(), "MEMBER_REMOVED", memberId);
            membersGUI.open(player, plot, holder.getPage());
        } else if (player.hasPermission("riftplots.members.edit")) new PlotPermissionsGUI().open(player, plot, member);
    }

    private void handlePermissions(Player player, PlotGUIHolder holder, int slot, ItemStack clicked) {
        Plot plot = plotManager.getPlot(holder.getPlotId()).orElse(null);
        if (plot == null || (!plot.isOwner(player.getUniqueId()) && !player.hasPermission("riftplots.admin.bypass")) || !player.hasPermission("riftplots.members.edit")) {
            player.closeInventory();
            return;
        }
        if (slot == 31) {
            membersGUI.open(player, plot);
            return;
        }
        PlotMember member = plot.getMember(holder.getMember());
        if (member == null || member.getRole() == pl.riftplots.domain.plot.PlotMemberRole.TENANT) {
            player.closeInventory();
            return;
        }
        String role = GuiData.get(clicked, "role");
        String permission = GuiData.get(clicked, "permission");
        if (role != null && (role.equals("MEMBER") || role.equals("MANAGER")))
            member.setRole(pl.riftplots.domain.plot.PlotMemberRole.valueOf(role));
        else if (permission != null && member.getRole() != pl.riftplots.domain.plot.PlotMemberRole.MANAGER) {
            var required = pl.riftplots.domain.plot.PlotPermission.valueOf(permission);
            if (member.hasPermission(required)) member.revokePermission(required);
            else member.grantPermission(required);
        } else return;
        plotManager.savePlot(plot);
        if (audit != null)
            audit.log(plot.getId(), player.getUniqueId(), player.getName(), "MEMBER_ACCESS_CHANGED", member.getPlayerUuid() + " " + member.getRole() + " " + member.getPermissions());
        new PlotPermissionsGUI().open(player, plot, member);
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
