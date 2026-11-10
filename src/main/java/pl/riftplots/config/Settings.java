package pl.riftplots.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.*;

/**
 * Snapshot of configurable policy, independent of Bukkit's mutable configuration cache.
 */
public final class Settings {
    private static volatile YamlConfiguration values = ConfigurationFiles.defaults("config.yml");

    private Settings() {
    }

    public static void install(ConfigurationSection configuration) {
        validate(configuration);
        values = ConfigurationFiles.merged("config.yml", configuration);
    }

    public static void validate(ConfigurationSection configuration) {
        var candidate = ConfigurationFiles.merged("config.yml", configuration);
        int decimals = candidate.getInt("economy.decimal-places");
        pl.riftplots.domain.plot.PlotCategory.valueOf(candidate.getString("plots.default-category"));
        pl.riftplots.domain.plot.TaxMode.valueOf(candidate.getString("economy.default-tax-mode"));
        pl.riftplots.domain.rental.RentalPeriodUnit.valueOf(candidate.getString("rental.default-unit"));
        if (decimals < 0 || decimals > 8) throw new IllegalArgumentException("economy.decimal-places");
        var maximum = new java.math.BigDecimal(candidate.getString("economy.maximum-transaction-amount"));
        if (maximum.signum() <= 0 || maximum.compareTo(pl.riftplots.util.Money.MAX) > 0)
            throw new IllegalArgumentException("economy.maximum-transaction-amount");
        if (java.math.RoundingMode.valueOf(candidate.getString("economy.rounding-mode")) == java.math.RoundingMode.UNNECESSARY)
            throw new IllegalArgumentException("economy.rounding-mode");
        for (String key : List.of("economy.default-price", "economy.default-base-tax", "city-budget.initial-balance", "notifications.sound-volume", "notifications.sound-pitch", "notifications.title-fade-in-ms", "notifications.title-stay-ms", "notifications.title-fade-out-ms", "tax.minimum-multiplier", "refunds.retry-multiplier", "visualization.border-particle-size")) {
            double value = candidate.getDouble(key);
            if (!Double.isFinite(value) || value < 0) throw new IllegalArgumentException(key);
        }
        if (candidate.getDouble("tax.minimum-multiplier") <= 0)
            throw new IllegalArgumentException("tax.minimum-multiplier");
        if (candidate.getDouble("refunds.retry-multiplier") < 1)
            throw new IllegalArgumentException("refunds.retry-multiplier");
        if (candidate.getDouble("visualization.border-particle-size") <= 0)
            throw new IllegalArgumentException("visualization.border-particle-size");
        if (candidate.getInt("interaction.sign-target-distance") < 1 || candidate.getInt("interaction.sign-target-distance") > 100)
            throw new IllegalArgumentException("interaction.sign-target-distance");
        var particle = org.bukkit.Particle.valueOf(candidate.getString("visualization.border-particle"));
        if (particle.getDataType() != Void.class && particle.getDataType() != org.bukkit.Particle.DustOptions.class)
            throw new IllegalArgumentException("visualization.border-particle");
        if (org.bukkit.Material.matchMaterial(candidate.getString("plots.default-icon")) == null)
            throw new IllegalArgumentException("plots.default-icon");
        for (String name : candidate.getConfigurationSection("paging").getKeys(false)) {
            int count = candidate.getInt("paging." + name);
            if (count < 1 || count > 100) throw new IllegalArgumentException("paging." + name);
        }
        for (String key : List.of("rental.default-periods", "billing.default-payment-periods", "refunds.retry-initial-seconds", "refunds.retry-maximum-seconds", "performance.io-shutdown-timeout-seconds", "performance.spatial-index-max-chunks-per-plot", "visualization.border-interval-ticks"))
            if (candidate.getLong(key) < 1) throw new IllegalArgumentException(key);
        if (candidate.getInt("rental.default-periods") > candidate.getInt("rental.max-periods"))
            throw new IllegalArgumentException("rental.default-periods");
        if (candidate.getLong("refunds.retry-maximum-seconds") < candidate.getLong("refunds.retry-initial-seconds"))
            throw new IllegalArgumentException("refunds.retry-maximum-seconds");
        var limits = candidate.getLongList("tax.size-tier-limits");
        var multipliers = candidate.getDoubleList("tax.size-tier-multipliers");
        if (limits.size() != multipliers.size()) throw new IllegalArgumentException("tax.size-tier-multipliers");
        long previous = 0;
        for (int i = 0; i < limits.size(); i++) {
            if (limits.get(i) <= previous || !Double.isFinite(multipliers.get(i)) || multipliers.get(i) < 0)
                throw new IllegalArgumentException("tax.size-tier-limits");
            previous = limits.get(i);
        }
        if (!Double.isFinite(candidate.getDouble("tax.size-tier-overflow-multiplier")) || candidate.getDouble("tax.size-tier-overflow-multiplier") < 0)
            throw new IllegalArgumentException("tax.size-tier-overflow-multiplier");
        for (String unit : candidate.getConfigurationSection("rental-period-seconds").getKeys(false))
            if (candidate.getLong("rental-period-seconds." + unit) < 1 || candidate.getLong("rental-period-seconds." + unit) > 31_536_000L)
                throw new IllegalArgumentException("rental-period-seconds." + unit);
        java.time.format.DateTimeFormatter.ofPattern(candidate.getString("formatting.date-pattern"));
        if (!candidate.getString("formatting.timezone").equalsIgnoreCase("system"))
            java.time.ZoneId.of(candidate.getString("formatting.timezone"));
        if (!candidate.getString("visualization.border-color").matches("#[0-9a-fA-F]{6}"))
            throw new IllegalArgumentException("visualization.border-color");
        for (String state : candidate.getConfigurationSection("signs.layouts").getKeys(false)) {
            var lines = candidate.getList("signs.layouts." + state);
            if (lines == null || lines.size() != 4 || lines.stream().anyMatch(line -> !(line instanceof String)))
                throw new IllegalArgumentException("signs.layouts." + state);
        }
        for (String key : List.of("members.default-permissions", "rental.default-tenant-permissions")) {
            for (Object permission : candidate.getList(key)) {
                if (!(permission instanceof String name) || pl.riftplots.domain.plot.PlotPermission.parse(name) == null)
                    throw new IllegalArgumentException(key);
            }
        }
        for (String key : candidate.getConfigurationSection("tab-completion").getKeys(false))
            if (!candidate.isList("tab-completion." + key) || candidate.getList("tab-completion." + key).stream().anyMatch(value -> !(value instanceof String)))
                throw new IllegalArgumentException("tab-completion." + key);
    }

    public static int integer(String key) {
        require(key);
        return values.getInt(key);
    }

    public static long longValue(String key) {
        require(key);
        return values.getLong(key);
    }

    public static double decimal(String key) {
        require(key);
        return values.getDouble(key);
    }

    public static boolean flag(String key) {
        require(key);
        return values.getBoolean(key);
    }

    public static String string(String key) {
        require(key);
        return values.getString(key);
    }

    public static List<?> list(String key) {
        require(key);
        return values.getList(key);
    }

    public static List<String> strings(String key) {
        require(key);
        return List.copyOf(values.getStringList(key));
    }

    public static ConfigurationSection section(String key) {
        require(key);
        return values.getConfigurationSection(key);
    }

    public static java.math.RoundingMode rounding() {
        return java.math.RoundingMode.valueOf(string("economy.rounding-mode"));
    }

    public static Set<pl.riftplots.domain.plot.PlotPermission> permissions(String key) {
        var result = java.util.EnumSet.noneOf(pl.riftplots.domain.plot.PlotPermission.class);
        for (String permission : strings(key))
            result.add(pl.riftplots.domain.plot.PlotPermission.valueOf(permission.toUpperCase(java.util.Locale.ROOT)));
        return result;
    }

    private static void require(String key) {
        if (!values.contains(key)) throw new IllegalArgumentException(key);
    }
}
