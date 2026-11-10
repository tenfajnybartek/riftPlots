package pl.riftplots.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.ItemStack;
import pl.riftplots.config.*;
import pl.riftplots.domain.plot.*;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.plot.PlotManager;

import java.util.*;

public class PlotGUIListener implements Listener {
    private final PlotManager plots;
    private final PaymentService payments;
    private final PlotListGUI list;
    private final PlotDetailsGUI details;
    private final PlotConfirmBuyGUI buy;
    private final PlotMembersGUI members;
    private pl.riftplots.service.rental.RentalService rentals;
    private pl.riftplots.service.auction.AuctionService auctions;
    private pl.riftplots.service.audit.AuditService audit;

    public PlotGUIListener(PlotManager plots, PaymentService payments, PlotListGUI list, PlotDetailsGUI details, PlotConfirmBuyGUI buy, PlotMembersGUI members) {
        this.plots = plots;
        this.payments = payments;
        this.list = list;
        this.details = details;
        this.buy = buy;
        this.members = members;
    }

    public void setMarketServices(pl.riftplots.service.rental.RentalService rentals, pl.riftplots.service.auction.AuctionService auctions, pl.riftplots.service.audit.AuditService audit) {
        this.rentals = rentals;
        this.auctions = auctions;
        this.audit = audit;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PlotGUIHolder holder)) return;
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) return;
        if (!(event.getWhoClicked() instanceof Player player) || !player.hasPermission("riftplots.user") || !player.hasPermission("riftplots.user.gui"))
            return;
        ItemStack item = event.getCurrentItem();
        if (item == null || item.getType() == Material.AIR) return;
        String action = GuiData.get(item, "action");
        if (action == null) return;
        var actor = plots.enterActor(player);
        try {
            if (holder.getPlotId() != null && (plots.isResetPending(holder.getPlotId()) || plots.financiallyBlocked(holder.getPlotId(), null))) {
                player.closeInventory();
                player.sendMessage(Texts.component("gui-messages.operation-blocked"));
                return;
            }
            switch (holder.getGuiType()) {
                case "PLOT_MARKET" -> market(player, holder, action, item);
                case "PLOT_DETAILS" -> details(player, holder, action);
                case "PLOT_CONFIRM_BUY" -> purchase(player, holder, action, item);
                case "PLOT_CONFIRM_RENT" -> rental(player, holder, action, item);
                case "PLOT_AUCTION" -> auction(player, holder, action, item);
                case "PLOT_MEMBERS" -> members(player, holder, action, item, event.isRightClick());
                case "PLOT_PERMISSIONS" -> permissions(player, holder, action, item);
            }
        } catch (RuntimeException error) {
            java.util.logging.Logger.getLogger("riftPlots").log(java.util.logging.Level.SEVERE, Texts.raw("gui-messages.failure-log"), error);
            player.closeInventory();
            player.sendMessage(Texts.component("gui-messages.failure"));
        } finally {
            plots.restoreActor(actor);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof PlotGUIHolder
                && event.getRawSlots().stream().anyMatch(slot -> slot < event.getView().getTopInventory().getSize()))
            event.setCancelled(true);
    }

    private Plot plot(Player player, PlotGUIHolder holder) {
        Plot plot = plots.getPlot(holder.getPlotId()).orElse(null);
        if (plot == null) player.closeInventory();
        return plot;
    }

    private boolean owner(Player player, Plot plot) {
        return plot.isOwner(player.getUniqueId()) || player.hasPermission("riftplots.admin.bypass");
    }

    private void market(Player player, PlotGUIHolder holder, String action, ItemStack item) {
        var view = holder.getMarketView();
        if (view == null) return;
        if (!action.equals("plot-entry")) {
            list.market().navigate(player, view, action);
            return;
        }
        var plot = plots.getPlot(GuiData.get(item, "plot")).orElse(null);
        if (plot == null) {
            list.market().open(player, view);
            return;
        }
        switch (view.tab()) {
            case OWNED -> {
                if (owner(player, plot)) details.open(player, plot);
                else if (rentals != null) player.performCommand("plots rent info " + plot.getId());
            }
            case SALE -> {
                if (plot.isAvailableForPurchase() && player.hasPermission("riftplots.user.buy")) buy.open(player, plot);
            }
            case RENT -> {
                if (player.hasPermission("riftplots.rent"))
                    list.market().confirmRental(player, plot, Settings.flag("rental.default-auto-renew"));
            }
            case AUCTION -> {
                if (player.hasPermission("riftplots.auction")) list.market().auction(player, plot);
            }
        }
    }

    private void details(Player player, PlotGUIHolder holder, String action) {
        var plot = plot(player, holder);
        if (plot == null || !owner(player, plot)) {
            player.closeInventory();
            return;
        }
        switch (action) {
            case "pay" -> {
                if (player.hasPermission("riftplots.user.pay")) {
                    payments.payTax(player, plot, Settings.integer("billing.default-payment-periods"));
                    details.open(player, plot);
                }
            }
            case "members" -> {
                if (player.hasPermission("riftplots.user.members")) members.open(player, plot);
            }
            case "abandon" -> {
                if (player.hasPermission("riftplots.user.abandon")) {
                    player.closeInventory();
                    player.performCommand("plots abandon " + plot.getId());
                }
            }
            case "back" -> list.open(player);
        }
    }

    private void purchase(Player player, PlotGUIHolder holder, String action, ItemStack item) {
        var plot = plot(player, holder);
        if (plot == null) return;
        switch (action) {
            case "confirm" -> {
                if (!player.hasPermission("riftplots.user.buy")) return;
                var quote = GuiData.get(item, "quote");
                var price = Optional.ofNullable(plots.getPurchasePrice(plot)).orElse(plot.getPrice());
                if (quote == null || new java.math.BigDecimal(quote).compareTo(price) != 0) {
                    buy.open(player, plot);
                    return;
                }
                player.closeInventory();
                payments.buyPlot(player, plot);
            }
            case "cancel" -> list.openAvailable(player);
            case "border" -> {
                if (player.hasPermission("riftplots.border")) {
                    player.closeInventory();
                    player.performCommand("plots border " + plot.getId());
                }
            }
        }
    }

    private void rental(Player player, PlotGUIHolder holder, String action, ItemStack item) {
        var plot = plot(player, holder);
        if (plot == null) return;
        switch (action) {
            case "cancel" -> list.market().open(player, MarketView.start(MarketView.Tab.RENT));
            case "autorenew" -> list.market().confirmRental(player, plot, !holder.isAutoRenew());
            case "confirm" -> {
                if (rentals == null || !player.hasPermission("riftplots.rent")) return;
                if (!MarketMenu.rentalTerms(plot).equals(GuiData.get(item, "terms"))) {
                    list.market().confirmRental(player, plot, holder.isAutoRenew());
                    return;
                }
                player.closeInventory();
                rentals.rentPlot(player, plot, Settings.integer("rental.default-periods"), holder.isAutoRenew());
            }
        }
    }

    private void auction(Player player, PlotGUIHolder holder, String action, ItemStack item) {
        var plot = plot(player, holder);
        if (plot == null) return;
        if (action.equals("back")) {
            list.market().open(player, MarketView.start(MarketView.Tab.AUCTION));
            return;
        }
        if (action.equals("history")) {
            player.closeInventory();
            player.performCommand("plots auction history " + plot.getId());
            return;
        }
        if ((!action.equals("bid") && !action.equals("buyout")) || auctions == null || !player.hasPermission("riftplots.auction"))
            return;
        var current = auctions.getAuction(plot.getId()).orElse(null);
        if (current == null || !current.getId().equals(GuiData.get(item, "auction"))) {
            list.market().auction(player, plot);
            return;
        }
        String quote = GuiData.get(item, "quote");
        if (quote == null) return;
        player.closeInventory();
        auctions.placeBid(player, plot.getId(), new java.math.BigDecimal(quote));
    }

    private void members(Player player, PlotGUIHolder holder, String action, ItemStack item, boolean rightClick) {
        var plot = plot(player, holder);
        if (plot == null || !owner(player, plot)) {
            player.closeInventory();
            return;
        }
        if (action.equals("back")) {
            details.open(player, plot);
            return;
        }
        if (action.equals("previous") || action.equals("next")) {
            members.open(player, plot, holder.getPage() + (action.equals("previous") ? -1 : 1));
            return;
        }
        if (!action.equals("member-entry")) return;
        var member = plot.getMember(UUID.fromString(GuiData.get(item, "member")));
        if (member == null) {
            members.open(player, plot, holder.getPage());
            return;
        }
        if (member.getRole() == PlotMemberRole.TENANT) {
            player.sendMessage(Texts.component("gui-messages.tenant-access"));
            return;
        }
        if (rightClick && player.hasPermission("riftplots.user.untrust")) {
            plot.removeMember(member.getPlayerUuid());
            plots.savePlot(plot);
            if (audit != null)
                audit.log(plot.getId(), player.getUniqueId(), player.getName(), "MEMBER_REMOVED", member.getPlayerUuid().toString());
            members.open(player, plot, holder.getPage());
        } else if (player.hasPermission("riftplots.members.edit")) new PlotPermissionsGUI().open(player, plot, member);
    }

    private void permissions(Player player, PlotGUIHolder holder, String action, ItemStack item) {
        var plot = plot(player, holder);
        if (plot == null || !owner(player, plot) || !player.hasPermission("riftplots.members.edit")) {
            player.closeInventory();
            return;
        }
        if (action.equals("back")) {
            members.open(player, plot);
            return;
        }
        var member = plot.getMember(holder.getMember());
        if (member == null || member.getRole() == PlotMemberRole.TENANT) {
            player.closeInventory();
            return;
        }
        if (action.equals("member") || action.equals("manager"))
            member.setRole(action.equals("member") ? PlotMemberRole.MEMBER : PlotMemberRole.MANAGER);
        else if (action.equals("permission-entry") && member.getRole() != PlotMemberRole.MANAGER) {
            var permission = PlotPermission.valueOf(GuiData.get(item, "permission"));
            if (member.hasPermission(permission)) member.revokePermission(permission);
            else member.grantPermission(permission);
        } else return;
        plots.savePlot(plot);
        if (audit != null)
            audit.log(plot.getId(), player.getUniqueId(), player.getName(), "MEMBER_ACCESS_CHANGED", member.getPlayerUuid() + " " + member.getRole() + " " + member.getPermissions());
        new PlotPermissionsGUI().open(player, plot, member);
    }
}
