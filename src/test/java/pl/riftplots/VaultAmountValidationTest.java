package pl.riftplots;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pl.riftplots.integration.vault.VaultHook;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VaultAmountValidationTest {
    @org.junit.jupiter.api.io.TempDir
    java.nio.file.Path directory;
    Economy economy;
    VaultHook vault;
    OfflinePlayer player;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setup() {
        Plugin plugin = mock(Plugin.class, RETURNS_DEEP_STUBS);
        economy = mock(Economy.class);
        player = mock(OfflinePlayer.class);
        when(player.getUniqueId()).thenReturn(java.util.UUID.randomUUID());
        RegisteredServiceProvider<Economy> provider = mock(RegisteredServiceProvider.class);
        when(plugin.getServer().getPluginManager().getPlugin("Vault")).thenReturn(mock(Plugin.class));
        when(plugin.getServer().getServicesManager().getRegistration(Economy.class)).thenReturn(provider);
        when(provider.getProvider()).thenReturn(economy);
        when(economy.has(eq(player), anyDouble())).thenReturn(true);
        EconomyResponse success = new EconomyResponse(1, 100, EconomyResponse.ResponseType.SUCCESS, null);
        when(economy.withdrawPlayer(eq(player), anyDouble())).thenReturn(success);
        when(economy.depositPlayer(eq(player), anyDouble())).thenReturn(success);
        vault = new VaultHook(plugin);
        assertTrue(vault.setup());
    }

    @Test
    void negativeOrMissingAmountsAreRejected() {
        for (BigDecimal amount : new BigDecimal[]{null, BigDecimal.ONE.negate()}) {
            assertFalse(vault.has(player, amount));
            assertFalse(vault.withdraw(player, amount));
            assertFalse(vault.deposit(player, amount));
        }
        verifyNoInteractions(economy);
    }

    @Test
    void overflowAndUnderflowNeverReachTheEconomyProvider() {
        for (BigDecimal amount : new BigDecimal[]{new BigDecimal("1e1000"), new BigDecimal("1e-1000")}) {
            assertFalse(vault.has(player, amount));
            assertFalse(vault.withdraw(player, amount));
            assertFalse(vault.deposit(player, amount));
        }
        verifyNoInteractions(economy);
    }

    @Test
    void zeroIsAValidFreeOperationWithoutCallingTheProvider() {
        assertTrue(vault.has(player, BigDecimal.ZERO));
        assertTrue(vault.withdraw(player, BigDecimal.ZERO));
        assertTrue(vault.deposit(player, BigDecimal.ZERO));
        verifyNoInteractions(economy);
    }

    @Test
    void normalPaymentsStillUseTheProvider() {
        BigDecimal amount = new BigDecimal("1.25");
        assertTrue(vault.has(player, amount));
        assertTrue(vault.withdraw(player, amount));
        assertTrue(vault.deposit(player, amount));
        verify(economy).has(player, 1.25);
        verify(economy).withdrawPlayer(player, 1.25);
        verify(economy).depositPlayer(player, 1.25);
    }

    @Test
    void realVaultHookRecordsBothSidesBeforeAServiceCommitFailureAndBlocksReplay() {
        var journal = new pl.riftplots.service.transaction.OperationJournal(directory.toFile());
        journal.initialize();
        vault.setOperationJournal(journal);
        assertThrows(IllegalStateException.class, () -> journal.execute("TRANSFER", "sale", "plot", player.getUniqueId(), BigDecimal.TEN, () -> {
            assertTrue(vault.withdraw(player, BigDecimal.TEN));
            assertTrue(vault.deposit(player, BigDecimal.TEN));
            throw new IllegalStateException("Ownership save failed after payout");
        }));
        var restarted = new pl.riftplots.service.transaction.OperationJournal(directory.toFile());
        restarted.initialize();
        var operation = restarted.latest("sale").orElseThrow();
        assertEquals(2, operation.steps().size());
        assertTrue(operation.steps().stream().allMatch(step -> step.state().equals("APPLIED")));
        assertTrue(restarted.isBlocked("plot"));
        assertFalse(restarted.execute("TRANSFER", "sale-again", "plot", player.getUniqueId(), BigDecimal.TEN, () -> vault.withdraw(player, BigDecimal.TEN)));
        verify(economy, times(1)).withdrawPlayer(player, 10d);
        verify(economy, times(1)).depositPlayer(player, 10d);
    }

    @Test
    void providerExceptionLeavesAnInFlightStepWithoutRepeatingTheCall() {
        var journal = new pl.riftplots.service.transaction.OperationJournal(directory.toFile());
        journal.initialize();
        vault.setOperationJournal(journal);
        when(economy.depositPlayer(player, 10d)).thenThrow(new IllegalStateException("Provider lost response"));
        assertThrows(IllegalStateException.class, () -> journal.execute("REFUND", "refund", "plot", player.getUniqueId(), BigDecimal.TEN,
                () -> vault.deposit(player, BigDecimal.TEN)));
        assertEquals("IN_FLIGHT", journal.latest("refund").orElseThrow().steps().getFirst().state());
        assertFalse(journal.execute("REFUND", "refund", "plot", player.getUniqueId(), BigDecimal.TEN, () -> vault.deposit(player, BigDecimal.TEN)));
        verify(economy, times(1)).depositPlayer(player, 10d);
    }
}
