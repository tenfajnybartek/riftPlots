package pl.riftplots;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pl.riftplots.api.PlotResetStartEvent;
import pl.riftplots.domain.plot.*;
import pl.riftplots.gui.*;
import pl.riftplots.integration.worldedit.WorldEditHook;
import pl.riftplots.integration.worldguard.WorldGuardHook;
import pl.riftplots.service.payment.PaymentService;
import pl.riftplots.service.plot.PlotManager;
import pl.riftplots.service.protection.PlotProtectionService;
import pl.riftplots.service.reset.PlotResetService;
import pl.riftplots.service.reset.PlotSnapshotService;
import pl.riftplots.storage.repository.PlotRepository;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProtectionRegressionTest {
    @TempDir
    Path directory;

    private PlotGUIListener gui(PlotManager plots, PaymentService payments) {
        return new PlotGUIListener(plots, payments, mock(PlotListGUI.class), mock(PlotDetailsGUI.class),
                mock(PlotConfirmBuyGUI.class), mock(PlotMembersGUI.class));
    }

    @Test
    void clicksInPlayerInventoryDoNotExecuteGuiActions() {
        PlotManager plots = mock(PlotManager.class);
        PaymentService payments = mock(PaymentService.class);
        var event = mock(InventoryClickEvent.class);
        var top = mock(Inventory.class);
        var view = mock(InventoryView.class);
        when(top.getHolder()).thenReturn(new PlotGUIHolder("PLOT_DETAILS", "test_plot"));
        when(top.getSize()).thenReturn(45);
        when(event.getInventory()).thenReturn(top);
        when(event.getView()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(top);
        when(event.getRawSlot()).thenReturn(76);
        when(event.getSlot()).thenReturn(31);
        gui(plots, payments).onInventoryClick(event);
        verify(event).setCancelled(true);
        verifyNoInteractions(plots, payments);
    }

    @Test
    void draggingItemsIntoGuiIsBlocked() {
        var event = mock(InventoryDragEvent.class);
        var view = mock(InventoryView.class);
        var inventory = mock(Inventory.class);
        when(event.getView()).thenReturn(view);
        when(view.getTopInventory()).thenReturn(inventory);
        when(inventory.getHolder()).thenReturn(new PlotGUIHolder("PLOT_LIST", null));
        when(inventory.getSize()).thenReturn(54);
        when(event.getRawSlots()).thenReturn(Set.of(0, 60));
        gui(mock(PlotManager.class), mock(PaymentService.class)).onInventoryDrag(event);
        verify(event).setCancelled(true);
    }

    @Test
    void oldOwnerCannotRemoveMembersThroughAnAlreadyOpenMenu() {
        Player previousOwner = mock(Player.class);
        when(previousOwner.getUniqueId()).thenReturn(UUID.randomUUID());
        when(previousOwner.hasPermission(anyString())).thenAnswer(call -> !call.getArgument(0).equals("riftplots.admin.bypass"));
        Plot plot = TestFixtures.plot(UUID.randomUUID());
        var plots = mock(PlotManager.class);
        when(plots.getPlot(plot.getId())).thenReturn(Optional.of(plot));
        var top = mock(Inventory.class);
        when(top.getHolder()).thenReturn(new PlotGUIHolder("PLOT_MEMBERS", plot.getId()));
        when(top.getSize()).thenReturn(45);
        var view = mock(InventoryView.class);
        when(view.getTopInventory()).thenReturn(top);
        var item = mock(ItemStack.class);
        when(item.getType()).thenReturn(Material.PLAYER_HEAD);
        var event = mock(InventoryClickEvent.class);
        when(event.getInventory()).thenReturn(top);
        when(event.getView()).thenReturn(view);
        when(event.getWhoClicked()).thenReturn(previousOwner);
        when(event.getCurrentItem()).thenReturn(item);
        gui(plots, mock(PaymentService.class)).onInventoryClick(event);
        verify(plots, never()).savePlot(any());
    }

    @Test
    void tenantCanBuildOnACityOwnedRentalWhileOtherPlayersCannot() {
        Player tenant = mock(Player.class);
        UUID tenantId = UUID.randomUUID();
        when(tenant.getUniqueId()).thenReturn(tenantId);
        Plot plot = TestFixtures.plot(null);
        plot.setActiveRentalId("rental");
        plot.addMember(new PlotMember(tenantId, "Tenant", PlotMemberRole.TENANT, EnumSet.of(PlotPermission.BUILD), Instant.now()));
        World world = mock(World.class);
        when(world.getName()).thenReturn("world");
        var plots = mock(PlotManager.class);
        when(plots.getPlotAt("world", 1, 65, 1)).thenReturn(Optional.of(plot));
        when(plots.hasUsableMembership(plot, tenantId)).thenReturn(true);
        var protection = new PlotProtectionService(plots);
        var location = new org.bukkit.Location(world, 1, 65, 1);
        assertTrue(protection.canInteract(tenant, location, PlotPermission.BUILD));
        Player stranger = mock(Player.class);
        when(stranger.getUniqueId()).thenReturn(UUID.randomUUID());
        assertFalse(protection.canInteract(stranger, location, PlotPermission.BUILD));
        assertFalse(protection.canInteract(tenant, location, PlotPermission.DESTROY));
    }

    @Test
    void missingSnapshotDoesNotSilentlyReleaseUnrestoredPlot() {
        Plot plot = TestFixtures.plot(UUID.randomUUID());
        UUID originalOwner = plot.getOwnerUuid();
        var plots = mock(PlotManager.class);
        var plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        var snapshots = new PlotSnapshotService(directory.toFile(), mock(WorldEditHook.class));
        var reset = new PlotResetService(plugin, TestFixtures.config(), plots, snapshots, mock(WorldEditHook.class));
        assertFalse(reset.startReset(plot, null));
        assertEquals(PlotStatus.DISABLED, plot.getStatus());
        assertEquals(originalOwner, plot.getOwnerUuid());
        assertFalse(reset.isResetting(plot.getId()));
    }

    @Test
    void cancellingResetDoesNotClearOwnership() throws Exception {
        Plot plot = TestFixtures.plot(UUID.randomUUID());
        UUID owner = plot.getOwnerUuid();
        var snapshots = new PlotSnapshotService(directory.toFile(), mock(WorldEditHook.class));
        Files.write(snapshots.getSchematicFile(plot.getId()).toPath(), new byte[]{1});
        var manager = mock(PluginManager.class);
        doAnswer(call -> {
            ((PlotResetStartEvent) call.getArgument(0)).setCancelled(true);
            return null;
        }).when(manager).callEvent(any());
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(manager);
            var reset = new PlotResetService(mock(Plugin.class), TestFixtures.config(), mock(PlotManager.class), snapshots, mock(WorldEditHook.class));
            assertFalse(reset.startReset(plot, null));
        }
        assertEquals(owner, plot.getOwnerUuid());
        assertEquals(PlotStatus.OWNED, plot.getStatus());
    }

    @Test
    void invalidSchematicDimensionsRejectResetBeforeOwnershipOrRentalChanges() throws Exception {
        Plot plot = TestFixtures.plot(UUID.randomUUID());
        UUID owner = plot.getOwnerUuid();
        var hook = mock(WorldEditHook.class);
        var snapshots = new PlotSnapshotService(directory.toFile(), hook);
        Files.write(snapshots.getSchematicFile(plot.getId()).toPath(), new byte[]{1});
        var clipboard = mock(com.sk89q.worldedit.extent.clipboard.Clipboard.class);
        var region = mock(com.sk89q.worldedit.regions.Region.class);
        when(clipboard.getRegion()).thenReturn(region);
        when(region.getWidth()).thenReturn(1);
        when(hook.loadSnapshot(any())).thenReturn(clipboard);
        var scheduler = mock(org.bukkit.scheduler.BukkitScheduler.class);
        doAnswer(call -> {
            ((Runnable) call.getArgument(1)).run();
            return mock(org.bukkit.scheduler.BukkitTask.class);
        }).when(scheduler).runTaskAsynchronously(any(Plugin.class), any(Runnable.class));
        doAnswer(call -> {
            ((Runnable) call.getArgument(1)).run();
            return mock(org.bukkit.scheduler.BukkitTask.class);
        }).when(scheduler).runTask(any(Plugin.class), any(Runnable.class));
        var plugin = mock(Plugin.class);
        when(plugin.getLogger()).thenReturn(Logger.getAnonymousLogger());
        var plots = mock(PlotManager.class);
        var rentals = mock(pl.riftplots.service.rental.RentalService.class);
        var auctions = mock(pl.riftplots.service.auction.AuctionService.class);
        var reset = new PlotResetService(plugin, TestFixtures.config(), plots, snapshots, hook);
        reset.setMarketServices(rentals, auctions);
        var callback = new java.util.concurrent.atomic.AtomicReference<Boolean>();
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getPluginManager).thenReturn(mock(PluginManager.class));
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(mock(World.class));
            assertTrue(reset.startReset(plot, callback::set));
        }
        assertEquals(Boolean.FALSE, callback.get());
        assertFalse(reset.isResetting(plot.getId()));
        assertEquals(owner, plot.getOwnerUuid());
        assertEquals(PlotStatus.OWNED, plot.getStatus());
        verify(plots, never()).savePlot(any());
        verifyNoInteractions(rentals, auctions);
    }

    @Test
    void advanceLimitRecoversAsPaidPeriodsElapse() {
        Plot plot = TestFixtures.plot(UUID.randomUUID());
        plot.setAdvancePeriodsPaid(4);
        plot.setNextTaxPayment(Instant.now().plus(Duration.ofDays(10)));
        assertEquals(1, plot.getAdvancePeriodsPaid());
        plot.setNextTaxPayment(Instant.now().plus(Duration.ofDays(3)));
        assertEquals(0, plot.getAdvancePeriodsPaid());
    }

    @Test
    void memberWithNoPermissionsAndAgreementsWithMissingDatesAreValid() {
        var member = new PlotMember(UUID.randomUUID(), "Guest", PlotMemberRole.MEMBER, Set.of(), null);
        assertTrue(member.getPermissions().isEmpty());
        var auction = new pl.riftplots.domain.auction.PlotAuction("id", "plot", null, BigDecimal.ONE, BigDecimal.ONE,
                null, null, null, null, null, null, List.of(), null);
        assertTrue(auction.getEndTime().isAfter(auction.getStartTime()));
        var rental = new pl.riftplots.domain.rental.RentalAgreement("id", "plot", null, UUID.randomUUID(), "Tenant",
                BigDecimal.ONE, null, null, null, null, false, null);
        assertTrue(rental.getEndDate().isAfter(rental.getStartDate()));
    }

    @Test
    void failedWorldGuardRegistrationDoesNotPersistAnUnprotectedPlot() {
        var repository = mock(PlotRepository.class);
        var hook = mock(WorldGuardHook.class);
        PlotManager plots = new PlotManager(repository, hook);
        assertFalse(plots.registerPlot(TestFixtures.plot(null)));
        assertTrue(plots.getAllPlots().isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void failedSnapshotWritePreservesThePreviousSchematic() throws Exception {
        Plot plot = TestFixtures.plot(null);
        var hook = mock(WorldEditHook.class);
        var snapshots = new PlotSnapshotService(directory.toFile(), hook);
        Path schematic = snapshots.getSchematicFile(plot.getId()).toPath();
        Files.writeString(schematic, "previous snapshot");
        when(hook.saveSnapshot(any(), any())).thenAnswer(call -> {
            Files.writeString(((java.io.File) call.getArgument(1)).toPath(), "partial write");
            throw new java.io.IOException("Disk failure");
        });
        assertThrows(java.io.IOException.class, () -> snapshots.createSnapshot(plot));
        assertEquals("previous snapshot", Files.readString(schematic));
    }

    @Test
    void cancelledTeleportRefundsItsCost() {
        var player = mock(Player.class);
        UUID owner = UUID.randomUUID();
        when(player.getUniqueId()).thenReturn(owner);
        Plot plot = TestFixtures.plot(owner);
        plot.setHomeLocation("world", 5, 65, 5, 0, 0);
        var world = mock(World.class);
        when(world.getName()).thenReturn("world");
        var feet = mock(org.bukkit.block.Block.class);
        var head = mock(org.bukkit.block.Block.class);
        var below = mock(org.bukkit.block.Block.class);
        when(world.getBlockAt(any(org.bukkit.Location.class))).thenReturn(feet);
        when(feet.getRelative(0, 1, 0)).thenReturn(head);
        when(feet.getRelative(0, -1, 0)).thenReturn(below);
        when(below.isSolid()).thenReturn(true);
        when(below.getType()).thenReturn(Material.STONE);
        when(feet.getType()).thenReturn(Material.AIR);
        when(head.getType()).thenReturn(Material.AIR);
        when(feet.isPassable()).thenReturn(true);
        when(head.isPassable()).thenReturn(true);
        var vault = mock(pl.riftplots.integration.vault.VaultHook.class);
        when(vault.has(any(), any())).thenReturn(true);
        when(vault.withdraw(any(), any())).thenReturn(true);
        var config = TestFixtures.config();
        var teleport = new pl.riftplots.service.teleport.TeleportService(mock(PlotManager.class), vault, config, TestFixtures.messages(config));
        try (var bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getWorld("world")).thenReturn(world);
            assertFalse(teleport.teleportHome(player, plot));
        }
        verify(player).teleport(any(org.bukkit.Location.class));
        verify(vault).deposit(player, config.getTeleportCost());
    }
}
