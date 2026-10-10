package pl.riftplots.config;

import org.bukkit.configuration.file.FileConfiguration;
import pl.riftplots.domain.plot.TaxMode;

import java.math.BigDecimal;
import java.util.List;

public class PluginConfiguration {
    private String prefix;
    private String currencySymbol;
    private boolean currencySymbolSuffix;

    private BigDecimal defaultPrice;
    private BigDecimal defaultBaseTax;
    private TaxMode defaultTaxMode;
    private double defaultTaxMultiplier;
    private int defaultBillingPeriodDays;
    private int defaultGracePeriodHours;
    private int maxAdvancePeriods;
    private double defaultAbandonRefundPercent;

    private int schedulerCheckIntervalSeconds;
    private List<Long> taxReminderHours;

    private boolean protectPistons;
    private boolean protectFluids;
    private boolean protectExplosions;
    private boolean protectFireSpread;
    private boolean protectArmorStands;
    private boolean protectItemFrames;

    private boolean autoResetOnExpire;
    private int resetBlockBatchSize;
    private int resetTickDelay;

    private boolean signsEnabled;
    private boolean signAutoUpdate;

    public void load(FileConfiguration config) {
        this.prefix = config.getString("general.prefix", "<gradient:#00c6ff:#0072ff><b>riftPlots</b></gradient> <gray>»</gray> ");
        this.currencySymbol = config.getString("general.currency-symbol", "$");
        this.currencySymbolSuffix = config.getBoolean("general.currency-symbol-suffix", true);

        this.defaultPrice = BigDecimal.valueOf(config.getDouble("economy.default-price", 25000.0));
        this.defaultBaseTax = BigDecimal.valueOf(config.getDouble("economy.default-base-tax", 1500.0));
        this.defaultTaxMode = TaxMode.parse(config.getString("economy.default-tax-mode", "FIXED"));
        this.defaultTaxMultiplier = config.getDouble("economy.default-tax-multiplier", 1.0);
        this.defaultBillingPeriodDays = Math.max(1, config.getInt("economy.default-billing-period-days", 7));
        this.defaultGracePeriodHours = Math.max(0, config.getInt("economy.default-grace-period-hours", 6));
        this.maxAdvancePeriods = Math.max(1, config.getInt("economy.max-advance-periods", 4));
        this.defaultAbandonRefundPercent = Math.max(0.0, Math.min(100.0, config.getDouble("economy.default-abandon-refund-percent", 50.0)));

        this.schedulerCheckIntervalSeconds = Math.max(10, config.getInt("scheduler.check-interval-seconds", 60));
        this.taxReminderHours = config.getLongList("scheduler.tax-reminder-hours");
        if (this.taxReminderHours.isEmpty()) {
            this.taxReminderHours = List.of(48L, 24L, 12L, 2L, 1L);
        }

        this.protectPistons = config.getBoolean("protection.prevent-piston-border-crossing", true);
        this.protectFluids = config.getBoolean("protection.prevent-fluid-border-crossing", true);
        this.protectExplosions = config.getBoolean("protection.prevent-explosions", true);
        this.protectFireSpread = config.getBoolean("protection.prevent-fire-spread", true);
        this.protectArmorStands = config.getBoolean("protection.protect-armor-stands", true);
        this.protectItemFrames = config.getBoolean("protection.protect-item-frames", true);

        this.autoResetOnExpire = config.getBoolean("reset.auto-reset-on-expire", true);
        this.resetBlockBatchSize = Math.max(100, config.getInt("reset.block-batch-size", 2500));
        this.resetTickDelay = Math.max(1, config.getInt("reset.tick-delay", 1));

        this.signsEnabled = config.getBoolean("signs.enabled", true);
        this.signAutoUpdate = config.getBoolean("signs.auto-update", true);
    }

    public String getPrefix() {
        return prefix;
    }

    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public boolean isCurrencySymbolSuffix() {
        return currencySymbolSuffix;
    }

    public String formatMoney(BigDecimal amount) {
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        String formatted = String.format("%,.2f", amount.doubleValue());
        if (currencySymbolSuffix) {
            return formatted + " " + currencySymbol;
        } else {
            return currencySymbol + formatted;
        }
    }

    public BigDecimal getDefaultPrice() {
        return defaultPrice;
    }

    public BigDecimal getDefaultBaseTax() {
        return defaultBaseTax;
    }

    public TaxMode getDefaultTaxMode() {
        return defaultTaxMode;
    }

    public double getDefaultTaxMultiplier() {
        return defaultTaxMultiplier;
    }

    public int getDefaultBillingPeriodDays() {
        return defaultBillingPeriodDays;
    }

    public int getDefaultGracePeriodHours() {
        return defaultGracePeriodHours;
    }

    public int getMaxAdvancePeriods() {
        return maxAdvancePeriods;
    }

    public double getDefaultAbandonRefundPercent() {
        return defaultAbandonRefundPercent;
    }

    public int getSchedulerCheckIntervalSeconds() {
        return schedulerCheckIntervalSeconds;
    }

    public List<Long> getTaxReminderHours() {
        return taxReminderHours;
    }

    public boolean isProtectPistons() {
        return protectPistons;
    }

    public boolean isProtectFluids() {
        return protectFluids;
    }

    public boolean isProtectExplosions() {
        return protectExplosions;
    }

    public boolean isProtectFireSpread() {
        return protectFireSpread;
    }

    public boolean isProtectArmorStands() {
        return protectArmorStands;
    }

    public boolean isProtectItemFrames() {
        return protectItemFrames;
    }

    public boolean isAutoResetOnExpire() {
        return autoResetOnExpire;
    }

    public int getResetBlockBatchSize() {
        return resetBlockBatchSize;
    }

    public int getResetTickDelay() {
        return resetTickDelay;
    }

    public boolean isSignsEnabled() {
        return signsEnabled;
    }

    public boolean isSignAutoUpdate() {
        return signAutoUpdate;
    }
}
