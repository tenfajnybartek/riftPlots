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

    private double transferFeePercent;
    private int transferOfferDurationSeconds;

    private double auctionCityFeePercent;
    private int auctionAntiSnipeSeconds;
    private BigDecimal auctionDefaultMinBidIncrement;
    private int auctionDefaultDurationMinutes;

    private boolean teleportEnabled;
    private BigDecimal teleportCost;
    private int teleportCooldownSeconds;

    private BigDecimal cityBudgetInitialBalance;
    private int graceWarningIntervalSeconds;
    private boolean publicHistory;
    private int maxRentalPeriods;
    private boolean auctionEscrowEnabled;

    public boolean isAuctionEscrowEnabled() {
        return auctionEscrowEnabled;
    }

    public int getGraceWarningIntervalSeconds() {
        return graceWarningIntervalSeconds;
    }

    public boolean isPublicHistory() {
        return publicHistory;
    }

    public int getMaxRentalPeriods() {
        return maxRentalPeriods;
    }

    private String signHeader;
    private String signRenewHeader;
    private String signForSaleAction;
    private String signSoldAction;
    private String signRenewAction;
    private String signResettingAction;
    private String signDisabledAction;

    public void load(FileConfiguration config) {
        config = ConfigurationFiles.merged("config.yml", config);
        this.prefix = config.getString("general.prefix");
        this.currencySymbol = config.getString("general.currency-symbol");
        this.currencySymbolSuffix = config.getBoolean("general.currency-symbol-suffix");

        this.defaultPrice = BigDecimal.valueOf(config.getDouble("economy.default-price"));
        this.defaultBaseTax = BigDecimal.valueOf(config.getDouble("economy.default-base-tax"));
        this.defaultTaxMode = TaxMode.parse(config.getString("economy.default-tax-mode"));
        this.defaultTaxMultiplier = config.getDouble("economy.default-tax-multiplier");
        this.defaultBillingPeriodDays = Math.max(1, config.getInt("economy.default-billing-period-days"));
        this.defaultGracePeriodHours = Math.max(0, config.getInt("economy.default-grace-period-hours"));
        this.maxAdvancePeriods = Math.max(1, config.getInt("economy.max-advance-periods"));
        this.defaultAbandonRefundPercent = Math.max(0.0, Math.min(100.0, config.getDouble("economy.default-abandon-refund-percent")));

        this.schedulerCheckIntervalSeconds = Math.max(10, config.getInt("scheduler.check-interval-seconds"));
        this.taxReminderHours = config.getLongList("scheduler.tax-reminder-hours");

        this.protectPistons = config.getBoolean("protection.prevent-piston-border-crossing");
        this.protectFluids = config.getBoolean("protection.prevent-fluid-border-crossing");
        this.protectExplosions = config.getBoolean("protection.prevent-explosions");
        this.protectFireSpread = config.getBoolean("protection.prevent-fire-spread");
        this.protectArmorStands = config.getBoolean("protection.protect-armor-stands");
        this.protectItemFrames = config.getBoolean("protection.protect-item-frames");

        this.autoResetOnExpire = config.getBoolean("reset.auto-reset-on-expire");
        this.resetBlockBatchSize = Math.max(100, config.getInt("reset.block-batch-size"));
        this.resetTickDelay = Math.max(1, config.getInt("reset.tick-delay"));

        this.signsEnabled = config.getBoolean("signs.enabled");
        this.signAutoUpdate = config.getBoolean("signs.auto-update");

        this.signHeader = config.getString("signs.formats.header");
        this.signRenewHeader = config.getString("signs.formats.renew-header");
        this.signForSaleAction = config.getString("signs.formats.for-sale-action");
        this.signSoldAction = config.getString("signs.formats.sold-action");
        this.signRenewAction = config.getString("signs.formats.renew-action");
        this.signResettingAction = config.getString("signs.formats.resetting-action");
        this.signDisabledAction = config.getString("signs.formats.disabled-action");

        this.transferFeePercent = Math.clamp(config.getDouble("market.transfer-fee-percent"), 0.0, 100.0);
        this.transferOfferDurationSeconds = Math.max(10, config.getInt("market.transfer-offer-duration-seconds"));

        this.auctionCityFeePercent = Math.clamp(config.getDouble("auction.city-fee-percent"), 0.0, 100.0);
        this.auctionAntiSnipeSeconds = Math.max(0, config.getInt("auction.anti-snipe-extension-seconds"));
        this.auctionDefaultMinBidIncrement = BigDecimal.valueOf(Math.max(0.01, config.getDouble("auction.default-min-bid-increment")));
        this.auctionDefaultDurationMinutes = Math.max(1, config.getInt("auction.default-duration-minutes"));

        this.teleportEnabled = config.getBoolean("teleport.enabled");
        this.teleportCost = BigDecimal.valueOf(Math.max(0.0, config.getDouble("teleport.cost")));
        this.teleportCooldownSeconds = Math.max(0, config.getInt("teleport.cooldown-seconds"));

        this.cityBudgetInitialBalance = BigDecimal.valueOf(config.getDouble("city-budget.initial-balance"));
        this.graceWarningIntervalSeconds = Math.max(60, config.getInt("scheduler.grace-warning-interval-seconds"));
        this.publicHistory = config.getBoolean("privacy.public-history");
        this.maxRentalPeriods = Math.clamp(config.getInt("rental.max-periods"), 1, 3650);
        this.auctionEscrowEnabled = config.getBoolean("auction.escrow-enabled");
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
        var locale = java.util.Locale.forLanguageTag(Settings.string("formatting.money-locale"));
        var formatter = java.text.NumberFormat.getNumberInstance(locale);
        formatter.setGroupingUsed(Settings.flag("formatting.money-grouping"));
        formatter.setMinimumFractionDigits(Settings.integer("economy.decimal-places"));
        formatter.setMaximumFractionDigits(Settings.integer("economy.decimal-places"));
        formatter.setRoundingMode(Settings.rounding());
        String formatted = formatter.format(amount);
        if (currencySymbolSuffix) {
            return Texts.raw("format.money.suffix", formatted, currencySymbol);
        } else {
            return Texts.raw("format.money.prefix", formatted, currencySymbol);
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

    public double getTransferFeePercent() {
        return transferFeePercent;
    }

    public int getTransferOfferDurationSeconds() {
        return transferOfferDurationSeconds;
    }

    public double getAuctionCityFeePercent() {
        return auctionCityFeePercent;
    }

    public int getAuctionAntiSnipeSeconds() {
        return auctionAntiSnipeSeconds;
    }

    public BigDecimal getAuctionDefaultMinBidIncrement() {
        return auctionDefaultMinBidIncrement;
    }

    public int getAuctionDefaultDurationMinutes() {
        return auctionDefaultDurationMinutes;
    }

    public boolean isTeleportEnabled() {
        return teleportEnabled;
    }

    public BigDecimal getTeleportCost() {
        return teleportCost;
    }

    public int getTeleportCooldownSeconds() {
        return teleportCooldownSeconds;
    }

    public BigDecimal getCityBudgetInitialBalance() {
        return cityBudgetInitialBalance;
    }

    public String getSignHeader() {
        return signHeader;
    }

    public String getSignRenewHeader() {
        return signRenewHeader;
    }

    public String getSignForSaleAction() {
        return signForSaleAction;
    }

    public String getSignSoldAction() {
        return signSoldAction;
    }

    public String getSignRenewAction() {
        return signRenewAction;
    }

    public String getSignResettingAction() {
        return signResettingAction;
    }

    public String getSignDisabledAction() {
        return signDisabledAction;
    }
}
