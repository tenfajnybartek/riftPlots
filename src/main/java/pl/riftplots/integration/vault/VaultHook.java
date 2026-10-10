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
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        return economy.has(player, amount.doubleValue());
    }

    public boolean withdraw(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault economy provider is not available!");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount.doubleValue());
        return response.transactionSuccess();
    }

    public boolean deposit(OfflinePlayer player, BigDecimal amount) {
        if (!isAvailable()) {
            throw new IllegalStateException("Vault economy provider is not available!");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return true;
        }
        EconomyResponse response = economy.depositPlayer(player, amount.doubleValue());
        return response.transactionSuccess();
    }

    public String format(BigDecimal amount) {
        if (!isAvailable()) {
            return (amount != null ? amount.toPlainString() : "0.00") + " $";
        }
        return economy.format(amount != null ? amount.doubleValue() : 0.0);
    }
}
