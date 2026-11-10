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
        types.clear();
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("types");
        if (section == null) {
            registerDefaults();
            return;
        }

        for (String key : section.getKeys(false)) {
            String path = "types." + key;
            String name = config.getString(path + ".display-name", key);
            String desc = config.getString(path + ".description", "");
            BigDecimal price = BigDecimal.valueOf(config.getDouble(path + ".default-price", 25000));
            BigDecimal tax = BigDecimal.valueOf(config.getDouble(path + ".default-base-tax", 1500));
            TaxMode taxMode = TaxMode.parse(config.getString(path + ".default-tax-mode", "FIXED"));
            double taxMult = config.getDouble(path + ".default-tax-multiplier", 1.0);
            int billingDays = config.getInt(path + ".default-billing-period-days", 7);
            int maxMembers = config.getInt(path + ".max-members", 5);
            boolean rental = config.getBoolean(path + ".rental-allowed", true);
            boolean sales = config.getBoolean(path + ".sales-allowed", true);
            double refund = config.getDouble(path + ".refund-percent", 50.0);
            Material icon = Material.matchMaterial(config.getString(path + ".icon-material", "BRICKS"));
            if (icon == null) icon = Material.BRICKS;

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
            types.put(key.toUpperCase(), propType);
        }

        if (types.isEmpty()) {
            registerDefaults();
        }
    }

    private void registerDefaults() {
        types.put("RESIDENTIAL", new PropertyType("RESIDENTIAL", "Działka Mieszkalna", "Nieruchomość przeznaczona pod dom lub mieszkanie", BigDecimal.valueOf(25000), BigDecimal.valueOf(1500), TaxMode.FIXED, 1.0, 7, 5, true, true, 50.0, Material.OAK_DOOR, Map.of("pvp", "deny")));
        types.put("COMMERCIAL", new PropertyType("COMMERCIAL", "Lokal Usługowy", "Nieruchomość przeznaczona pod sklep lub usługi miejskie", BigDecimal.valueOf(45000), BigDecimal.valueOf(3000), TaxMode.FIXED, 1.25, 7, 10, true, true, 40.0, Material.EMERALD, Map.of("pvp", "deny")));
        types.put("INDUSTRIAL", new PropertyType("INDUSTRIAL", "Obiekt Przemysłowy", "Magazyn, fabryka lub działka przemysłowa", BigDecimal.valueOf(60000), BigDecimal.valueOf(4500), TaxMode.PER_CHUNK, 1.5, 14, 15, false, true, 30.0, Material.IRON_BLOCK, Map.of("pvp", "deny")));
        types.put("PREMIUM", new PropertyType("PREMIUM", "Posiadłość Premium", "Ekskluzywna rezydencja o obniżonych podatkach", BigDecimal.valueOf(100000), BigDecimal.valueOf(2000), TaxMode.FIXED, 0.8, 30, 20, true, true, 70.0, Material.DIAMOND_BLOCK, Map.of("pvp", "deny")));
        types.put("RENTAL", new PropertyType("RENTAL", "Lokal na Wynajem", "Nieruchomość miejska dedykowana pod krótko- i długoterminowy wynajem", BigDecimal.valueOf(30000), BigDecimal.valueOf(1000), TaxMode.FIXED, 1.0, 7, 3, true, false, 50.0, Material.CHEST, Map.of("pvp", "deny")));
        types.put("CUSTOM", new PropertyType("CUSTOM", "Nieruchomość Specjalna", "Działka o indywidualnych parametrach", BigDecimal.valueOf(25000), BigDecimal.valueOf(1500), TaxMode.CUSTOM, 1.0, 7, 10, true, true, 50.0, Material.BEACON, Map.of()));
    }

    private void createDefaultConfig() {
        YamlConfiguration config = new YamlConfiguration();
        registerDefaults();
        for (PropertyType type : types.values()) {
            String path = "types." + type.getId();
            config.set(path + ".display-name", type.getDisplayName());
            config.set(path + ".description", type.getDescription());
            config.set(path + ".default-price", type.getDefaultPrice().doubleValue());
            config.set(path + ".default-base-tax", type.getDefaultBaseTax().doubleValue());
            config.set(path + ".default-tax-mode", type.getDefaultTaxMode().name());
            config.set(path + ".default-tax-multiplier", type.getDefaultTaxMultiplier());
            config.set(path + ".default-billing-period-days", type.getDefaultBillingPeriodDays());
            config.set(path + ".max-members", type.getMaxMembers());
            config.set(path + ".rental-allowed", type.isRentalAllowed());
            config.set(path + ".sales-allowed", type.isSalesAllowed());
            config.set(path + ".refund-percent", type.getRefundPercent());
            config.set(path + ".icon-material", type.getIconMaterial().name());
            type.getDefaultFlags().forEach((flag, value) -> config.set(path + ".flags." + flag, value));
        }
        try {
            configFile.getParentFile().mkdirs();
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public PropertyType getType(String id) {
        if (id == null) {
            return types.get("RESIDENTIAL");
        }
        PropertyType type = types.get(id.toUpperCase());
        return type != null ? type : types.get("RESIDENTIAL");
    }

    public Collection<PropertyType> getAllTypes() {
        return Collections.unmodifiableCollection(types.values());
    }

    public boolean exists(String id) {
        return id != null && types.containsKey(id.toUpperCase());
    }
}
