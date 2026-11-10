package pl.riftplots.integration.vault;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.math.BigDecimal;
import java.util.Objects;

public class VaultHook {
    private final Plugin plugin;
    private Economy economy;
    private pl.riftplots.service.transaction.OperationJournal journal;

    public void setOperationJournal(pl.riftplots.service.transaction.OperationJournal journal) {
        this.journal = journal;
    }

    public VaultHook(Plugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin cannot be null");
    }

    public boolean setup() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        this.economy = rsp.getProvider();
        return this.economy != null;
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public BigDecimal getBalance(OfflinePlayer player) {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault economy provider is not available!");
        }
        double balance = economy.getBalance(player);
        return BigDecimal.valueOf(balance);
    }

    public boolean has(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault economy provider is not available!");
        }
        if (!isValidAmount(amount)) return false;
        if (amount.signum() == 0) {
            return true;
        }
        return economy.has(player, amount.doubleValue());
    }

    public boolean withdraw(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault economy provider is not available!");
        }
        if (!isValidAmount(amount)) return false;
        if (amount.signum() == 0) {
            return true;
        }
        if (journal != null && journal.isRunning()) {
            return journal.effect("WITHDRAW", player.getUniqueId(), amount,
                    () -> economy.withdrawPlayer(player, amount.doubleValue()).transactionSuccess());
        }
        return economy.withdrawPlayer(player, amount.doubleValue()).transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault economy provider is not available!");
        }
        if (!isValidAmount(amount)) return false;
        if (amount.signum() == 0) {
            return true;
        }
        if (journal != null && journal.isRunning()) {
            return journal.effect("DEPOSIT", player.getUniqueId(), amount,
                    () -> economy.depositPlayer(player, amount.doubleValue()).transactionSuccess());
        }
        return economy.depositPlayer(player, amount.doubleValue()).transactionSuccess();
    }

    private boolean isValidAmount(BigDecimal amount) {
        if (amount == null || amount.signum() < 0) return false;
        double value = amount.doubleValue();
        return Double.isFinite(value) && (amount.signum() == 0 || value > 0.0);
    }

    public String format(BigDecimal amount) {
        if (!isAvailable()) {
            return (amount != null ? amount.toPlainString() : "0.00") + " $";
        }
        return economy.format(amount != null ? amount.doubleValue() : 0.0);
    }
}
