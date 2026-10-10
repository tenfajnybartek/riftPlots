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
        districts.clear();
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        ConfigurationSection section = config.getConfigurationSection("districts");
        if (section == null) {
            registerDefaults();
            return;
        }

        for (String key : section.getKeys(false)) {
            String path = "districts." + key;
            String name = config.getString(path + ".name", key);
            String desc = config.getString(path + ".description", "");
            double priceMult = config.getDouble(path + ".price-multiplier", 1.0);
            double taxMult = config.getDouble(path + ".tax-multiplier", 1.0);
            List<String> types = config.getStringList(path + ".allowed-types");
            int maxPlots = config.getInt(path + ".max-plots-per-player", 3);
            boolean sales = config.getBoolean(path + ".sales-enabled", true);
            boolean rental = config.getBoolean(path + ".rental-enabled", true);

            District district = new District(key, name, desc, priceMult, taxMult, new HashSet<>(types), maxPlots, sales, rental);
            districts.put(key.toLowerCase(), district);
        }

        if (districts.isEmpty()) {
            registerDefaults();
        }
    }

    private void registerDefaults() {
        districts.put("centrum", new District("centrum", "Centrum Miasta", "Reprezentacyjne centrum z najwyższym prestiżem", 1.5, 1.25, Set.of("COMMERCIAL", "PREMIUM", "RESIDENTIAL"), 2, true, true));
        districts.put("stare_miasto", new District("stare_miasto", "Stare Miasto", "Zabytkowa część miasta z kamienicami", 1.2, 1.1, Set.of("RESIDENTIAL", "COMMERCIAL"), 2, true, true));
        districts.put("przedmiescia", new District("przedmiescia", "Przedmieścia", "Spokojna strefa domów jednorodzinnych", 0.8, 0.75, Set.of("RESIDENTIAL"), 4, true, true));
        districts.put("handlowa", new District("handlowa", "Dzielnica Handlowa", "Strefa targowa, centra handlowe i salony", 1.3, 1.3, Set.of("COMMERCIAL", "RENTAL"), 3, true, true));
        districts.put("przemyslowa", new District("przemyslowa", "Strefa Przemysłowa", "Fabryki, magazyny i zakłady produkcyjne", 0.9, 0.9, Set.of("INDUSTRIAL"), 5, true, false));
    }

    private void createDefaultConfig() {
        YamlConfiguration config = new YamlConfiguration();
        registerDefaults();
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
            configFile.getParentFile().mkdirs();
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
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
        return base.setScale(2, RoundingMode.HALF_UP);
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
        return base.setScale(2, RoundingMode.HALF_UP);
    }
}
