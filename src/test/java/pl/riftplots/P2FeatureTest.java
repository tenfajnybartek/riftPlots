package pl.riftplots;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.*;
import org.junit.jupiter.api.Test;
import pl.riftplots.domain.plot.*;
import pl.riftplots.gui.*;
import pl.riftplots.service.payment.*;
import pl.riftplots.service.plot.*;
import pl.riftplots.service.visualization.BorderService;
import pl.riftplots.command.BillingArguments;
import pl.riftplots.util.Money;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class P2FeatureTest {
    @Test
    void joinThatFinishesAfterMigrationStartsCannotChangeTheSourceSnapshot() {
        var profiles = mock(pl.riftplots.storage.repository.PlayerProfileRepository.class);
        var notifications = mock(pl.riftplots.service.notification.NotificationService.class);
        var config = TestFixtures.config();
        var listener = new pl.riftplots.listener.PlotSessionListener(config, TestFixtures.messages(config), mock(PlotManager.class), new TaxCalculator(), profiles, notifications);
        listener.setWriteAllowed(() -> false);
        var event = mock(org.bukkit.event.player.PlayerJoinEvent.class);
        var player = mock(Player.class);
        when(event.getPlayer()).thenReturn(player);
        listener.onPlayerJoin(event);
        verify(player).kick(any(net.kyori.adventure.text.Component.class));
        verifyNoInteractions(profiles, notifications);
    }

    @Test
    void filtersAndSortingKeepEveryMatchingPropertyReachable() {
        PlotManager plots = mock(PlotManager.class);
        UUID owner = UUID.randomUUID();
        List<Plot> properties = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            Plot plot = mock(Plot.class);
            when(plot.getId()).thenReturn("plot_" + String.format("%03d", i));
            when(plot.isAvailableForPurchase()).thenReturn(true);
            when(plot.getPrice()).thenReturn(BigDecimal.valueOf(100 - i));
            when(plot.getDistrictId()).thenReturn(i % 2 == 0 ? "center" : "suburbs");
            when(plot.getPropertyType()).thenReturn("RESIDENTIAL");
            properties.add(plot);
        }
        when(plots.getAllPlots()).thenReturn(properties);
        MarketMenu menu = new MarketMenu(TestFixtures.config(), plots, new TaxCalculator());
        var all = menu.matching(owner, MarketView.start(MarketView.Tab.SALE));
        assertEquals(100, all.size());
        assertEquals(100, all.stream().map(Plot::getId).distinct().count());
        var filtered = menu.matching(owner, new MarketView(MarketView.Tab.SALE, 0, MarketView.Sort.PRICE, "center", "RESIDENTIAL"));
        assertEquals(50, filtered.size());
        assertEquals("plot_098", filtered.getFirst().getId());
        assertEquals("plot_000", filtered.getLast().getId());
    }

    @Test
    void borderPointsRespectInclusiveVerticalBoundsAndStayBounded() {
        PlotBounds bounds = new PlotBounds("world", -10, 60, -20, 10, 64, 20);
        var points = BorderService.points(bounds, 120);
        assertEquals(120, points.size());
        assertTrue(points.stream().allMatch(p -> p.x() >= -10 && p.x() <= 11 && p.y() >= 60 && p.y() <= 65 && p.z() >= -20 && p.z() <= 21));
        assertTrue(points.stream().anyMatch(p -> p.y() == 60));
        assertTrue(points.stream().anyMatch(p -> p.y() == 65));
    }

    @Test
    void spatialIndexUpdatesOldChunksAndRespectsWorldCase() {
        PlotSpatialIndex index = new PlotSpatialIndex();
        index.put("flat", new PlotBounds("World", -17, 60, -1, -1, 65, 1));
        assertTrue(index.candidates("world", -16, 0).contains("flat"));
        index.put("flat", new PlotBounds("world", 32, 70, 32, 40, 75, 40));
        assertFalse(index.candidates("World", -16, 0).contains("flat"));
        assertTrue(index.candidates("WORLD", 35, 35).contains("flat"));
        index.remove("flat");
        assertTrue(index.candidates("world", 35, 35).isEmpty());
    }

    @Test
    void invalidPeriodsAndMoneyAreRejectedInsteadOfFallingBackToPayment() {
        for (String value : List.of("-1", "0", "2x", "2147483648"))
            assertThrows(IllegalArgumentException.class, () -> BillingArguments.positiveInt(value));
        for (String value : List.of("-1", "0.009", "1e1000", "1e-1000"))
            assertThrows(NumberFormatException.class, () -> Money.parse(value));
        assertEquals(new BigDecimal("1.25"), Money.parse("1.25"));
        PlotManager plots = mock(PlotManager.class);
        when(plots.getPlot("test_plot")).thenReturn(Optional.of(TestFixtures.plot(null)));
        assertThrows(IllegalArgumentException.class, () -> BillingArguments.resolve(mock(Player.class), new String[]{"test_plot", "wrong"}, plots));
    }

    @Test
    void changedPriceRequiresASecondConfirmation() {
        Plot plot = TestFixtures.plot(null);
        PlotManager plots = mock(PlotManager.class);
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        PaymentService payments = mock(PaymentService.class);
        PlotConfirmBuyGUI confirmation = mock(PlotConfirmBuyGUI.class);
        var listener = new PlotGUIListener(plots, payments, mock(PlotListGUI.class), mock(PlotDetailsGUI.class), confirmation, mock(PlotMembersGUI.class));
        Player player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        Inventory inventory = mock(Inventory.class);
        when(inventory.getHolder()).thenReturn(new PlotGUIHolder("PLOT_CONFIRM_BUY", plot.getId()));
        when(inventory.getSize()).thenReturn(27);
        InventoryView view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        ItemStack item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.LIME_WOOL);
        when(item.hasItemMeta()).thenReturn(true);
        ItemMeta meta = mock(ItemMeta.class);
        when(item.getItemMeta()).thenReturn(meta);
        PersistentDataContainer data = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(any(), eq(PersistentDataType.STRING))).thenAnswer(call ->
                ((org.bukkit.NamespacedKey) call.getArgument(0)).getKey().equals("action") ? "confirm" : "999");
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getRawSlot()).thenReturn(11);
        when(event.getSlot()).thenReturn(11);
        when(event.getCurrentItem()).thenReturn(item);
        listener.onInventoryClick(event);
        verify(confirmation).open(player, plot);
        verifyNoInteractions(payments);
    }

    @Test
    void oldAuctionWindowCannotBidInAReplacementAuctionForTheSamePlot() {
        var plot = TestFixtures.plot(null);
        var plots = mock(PlotManager.class);
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        var auctions = mock(pl.riftplots.service.auction.AuctionService.class);
        var current = mock(pl.riftplots.domain.auction.PlotAuction.class);
        when(current.getId()).thenReturn("replacement");
        when(auctions.getAuction(plot.getId())).thenReturn(Optional.of(current));
        var list = mock(PlotListGUI.class);
        var market = mock(MarketMenu.class);
        when(list.market()).thenReturn(market);
        var listener = new PlotGUIListener(plots, mock(PaymentService.class), list, mock(PlotDetailsGUI.class), mock(PlotConfirmBuyGUI.class), mock(PlotMembersGUI.class));
        listener.setMarketServices(null, auctions, null);
        var player = mock(Player.class);
        when(player.hasPermission(anyString())).thenReturn(true);
        var inventory = mock(Inventory.class);
        when(inventory.getSize()).thenReturn(27);
        when(inventory.getHolder()).thenReturn(new PlotGUIHolder("PLOT_AUCTION", plot.getId()));
        var view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(inventory);
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.EMERALD);
        when(item.hasItemMeta()).thenReturn(true);
        var meta = mock(ItemMeta.class);
        when(item.getItemMeta()).thenReturn(meta);
        var data = mock(PersistentDataContainer.class);
        when(meta.getPersistentDataContainer()).thenReturn(data);
        when(data.get(any(), eq(PersistentDataType.STRING))).thenAnswer(call ->
                switch (((org.bukkit.NamespacedKey) call.getArgument(0)).getKey()) {
                    case "action" -> "buyout";
                    case "quote" -> "100";
                    default -> "previous";
                });
        var event = mock(InventoryClickEvent.class);
        when(event.getWhoClicked()).thenReturn(player);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getView()).thenReturn(view);
        when(event.getRawSlot()).thenReturn(15);
        when(event.getSlot()).thenReturn(15);
        when(event.getCurrentItem()).thenReturn(item);
        listener.onInventoryClick(event);
        verify(auctions, never()).placeBid(any(), anyString(), any());
        verify(market).auction(player, plot);
    }
}
