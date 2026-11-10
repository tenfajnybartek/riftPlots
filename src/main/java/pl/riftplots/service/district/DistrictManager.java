package pl.riftplots.service.district;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.domain.district.District;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.type.PropertyType;
import pl.riftplots.domain.type.PropertyTypeManager;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

public class DistrictManager {
    private final File configFile;
    private final PropertyTypeManager typeManager;
    private final Map<String, District> districts = new HashMap<>();

    public DistrictManager(File dataFolder, PropertyTypeManager typeManager) {
        this.configFile = new File(dataFolder, "districts.yml");
        this.typeManager = typeManager;
    }

    public void load() {
        Map<String, District> loaded = new HashMap<>();
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        YamlConfiguration config = pl.riftplots.config.ConfigurationFiles.read(configFile);
        ConfigurationSection section = config.getConfigurationSection("districts");
        if (section == null) {
            throw new IllegalArgumentException("districts.yml: districts");
        }

        for (String key : section.getKeys(false)) {
            String path = "districts." + key;
            String name = config.getString(path + ".name", key);
            String desc = config.getString(path + ".description", "");
            double priceMult = config.getDouble(path + ".price-multiplier", pl.riftplots.config.Settings.decimal("district-defaults.price-multiplier"));
            double taxMult = config.getDouble(path + ".tax-multiplier", pl.riftplots.config.Settings.decimal("district-defaults.tax-multiplier"));
            List<String> types = config.getStringList(path + ".allowed-types");
            int maxPlots = config.getInt(path + ".max-plots-per-player", pl.riftplots.config.Settings.integer("district-defaults.max-plots-per-player"));
            boolean sales = config.getBoolean(path + ".sales-enabled", pl.riftplots.config.Settings.flag("district-defaults.sales-enabled"));
            boolean rental = config.getBoolean(path + ".rental-enabled", pl.riftplots.config.Settings.flag("district-defaults.rental-enabled"));

            District district = new District(key, name, desc, priceMult, taxMult, new HashSet<>(types), maxPlots, sales, rental);
            loaded.put(key.toLowerCase(java.util.Locale.ROOT), district);
        }

        districts.clear();
        districts.putAll(loaded);
    }

    private void createDefaultConfig() {
        configFile.getParentFile().mkdirs();
        try {
            pl.riftplots.storage.yaml.YamlDataFile.save(pl.riftplots.config.ConfigurationFiles.defaults("districts.yml"), configFile);
        } catch (IOException e) {
            throw new IllegalStateException("districts.yml", e);
        }
    }

    public Optional<District> getDistrict(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(districts.get(id.toLowerCase()));
    }

    public Collection<District> getAllDistricts() {
        return Collections.unmodifiableCollection(districts.values());
    }

    public void addDistrict(District district) {
        if (district != null) {
            districts.put(district.getId().toLowerCase(), district);
            saveDistricts();
        }
    }

    public void removeDistrict(String id) {
        if (id != null && districts.remove(id.toLowerCase()) != null) {
            saveDistricts();
        }
    }

    public void saveDistricts() {
        YamlConfiguration config = new YamlConfiguration();
        for (District district : districts.values()) {
            String path = "districts." + district.getId();
            config.set(path + ".name", district.getName());
            config.set(path + ".description", district.getDescription());
            config.set(path + ".price-multiplier", district.getPriceMultiplier());
            config.set(path + ".tax-multiplier", district.getTaxMultiplier());
            config.set(path + ".allowed-types", new ArrayList<>(district.getAllowedPropertyTypes()));
            config.set(path + ".max-plots-per-player", district.getMaxPlotsPerPlayer());
            config.set(path + ".sales-enabled", district.isSalesEnabled());
            config.set(path + ".rental-enabled", district.isRentalEnabled());
        }
        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public BigDecimal resolveEffectivePrice(Plot plot) {
        if (plot == null) {
            return BigDecimal.ZERO;
        }
        if (plot.getPrice() != null && plot.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return plot.getPrice();
        }

        PropertyType type = typeManager.getType(plot.getPropertyType());
        BigDecimal base = type.getDefaultPrice();

        if (plot.getDistrictId() != null) {
            Optional<District> optDist = getDistrict(plot.getDistrictId());
            if (optDist.isPresent()) {
                base = base.multiply(BigDecimal.valueOf(optDist.get().getPriceMultiplier()));
            }
        }
        return base.setScale(pl.riftplots.config.Settings.integer("economy.decimal-places"), pl.riftplots.config.Settings.rounding());
    }

    public BigDecimal resolveEffectiveTax(Plot plot) {
        if (plot == null) {
            return BigDecimal.ZERO;
        }
        if (plot.getBaseTax() != null && plot.getBaseTax().compareTo(BigDecimal.ZERO) > 0) {
            return plot.getBaseTax();
        }

        PropertyType type = typeManager.getType(plot.getPropertyType());
        BigDecimal base = type.getDefaultBaseTax();

        if (plot.getDistrictId() != null) {
            Optional<District> optDist = getDistrict(plot.getDistrictId());
            if (optDist.isPresent()) {
                base = base.multiply(BigDecimal.valueOf(optDist.get().getTaxMultiplier()));
            }
        }
        return base.setScale(pl.riftplots.config.Settings.integer("economy.decimal-places"), pl.riftplots.config.Settings.rounding());
    }
}
