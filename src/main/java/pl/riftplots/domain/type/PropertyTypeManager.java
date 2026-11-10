package pl.riftplots.domain.type;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.domain.plot.TaxMode;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

public class PropertyTypeManager {
    private final File configFile;
    private final Map<String, PropertyType> types = new HashMap<>();

    public PropertyTypeManager(File dataFolder) {
        this.configFile = new File(dataFolder, "plot-types.yml");
    }

    public void load() {
        Map<String, PropertyType> loaded = new HashMap<>();
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        YamlConfiguration config = pl.riftplots.config.ConfigurationFiles.merged("plot-types.yml", pl.riftplots.config.ConfigurationFiles.read(configFile));
        ConfigurationSection section = config.getConfigurationSection("types");
        if (section == null) {
            throw new IllegalArgumentException("plot-types.yml: types");
        }

        for (String key : section.getKeys(false)) {
            String path = "types." + key;
            String name = config.getString(path + ".display-name", key);
            String desc = config.getString(path + ".description", "");
            BigDecimal price = BigDecimal.valueOf(config.getDouble(path + ".default-price", pl.riftplots.config.Settings.decimal("economy.default-price")));
            BigDecimal tax = BigDecimal.valueOf(config.getDouble(path + ".default-base-tax", pl.riftplots.config.Settings.decimal("economy.default-base-tax")));
            TaxMode taxMode = TaxMode.parse(config.getString(path + ".default-tax-mode", pl.riftplots.config.Settings.string("economy.default-tax-mode")));
            double taxMult = config.getDouble(path + ".default-tax-multiplier", pl.riftplots.config.Settings.decimal("economy.default-tax-multiplier"));
            int billingDays = config.getInt(path + ".default-billing-period-days", pl.riftplots.config.Settings.integer("economy.default-billing-period-days"));
            int maxMembers = config.getInt(path + ".max-members", pl.riftplots.config.Settings.integer("plots.default-max-members"));
            boolean rental = config.getBoolean(path + ".rental-allowed", pl.riftplots.config.Settings.flag("plots.rental-allowed"));
            boolean sales = config.getBoolean(path + ".sales-allowed", pl.riftplots.config.Settings.flag("plots.sales-allowed"));
            double refund = config.getDouble(path + ".refund-percent", pl.riftplots.config.Settings.decimal("economy.default-abandon-refund-percent"));
            Material icon = Material.matchMaterial(config.getString(path + ".icon-material", pl.riftplots.config.Settings.string("plots.default-icon")));
            if (icon == null || icon == Material.AIR || org.bukkit.Bukkit.getServer() != null && !icon.isItem())
                throw new IllegalArgumentException(path + ".icon-material");

            Map<String, String> flags = new HashMap<>();
            ConfigurationSection flagsSec = config.getConfigurationSection(path + ".flags");
            if (flagsSec != null) {
                for (String fKey : flagsSec.getKeys(false)) {
                    flags.put(fKey, flagsSec.getString(fKey));
                }
            }

            PropertyType propType = new PropertyType(
                    key, name, desc, price, tax, taxMode, taxMult,
                    billingDays, maxMembers, rental, sales, refund, icon, flags
            );
            loaded.put(key.toUpperCase(java.util.Locale.ROOT), propType);
        }

        if (loaded.isEmpty() || !loaded.containsKey(pl.riftplots.config.Settings.string("plots.default-type").toUpperCase(java.util.Locale.ROOT)))
            throw new IllegalArgumentException("plots.default-type");
        types.clear();
        types.putAll(loaded);
    }

    private void createDefaultConfig() {
        configFile.getParentFile().mkdirs();
        try {
            pl.riftplots.storage.yaml.YamlDataFile.save(pl.riftplots.config.ConfigurationFiles.defaults("plot-types.yml"), configFile);
        } catch (IOException e) {
            throw new IllegalStateException("plot-types.yml", e);
        }
    }

    public PropertyType getType(String id) {
        if (id == null) {
            return types.get(pl.riftplots.config.Settings.string("plots.default-type").toUpperCase(java.util.Locale.ROOT));
        }
        PropertyType type = types.get(id.toUpperCase());
        return type != null ? type : types.get(pl.riftplots.config.Settings.string("plots.default-type").toUpperCase(java.util.Locale.ROOT));
    }

    public Collection<PropertyType> getAllTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public boolean exists(String id) {
        return id != null && types.containsKey(id.toUpperCase());
    }
}
