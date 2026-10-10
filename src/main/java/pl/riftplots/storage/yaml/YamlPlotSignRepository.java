package pl.riftplots.storage.yaml;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.domain.sign.PlotSign;
import pl.riftplots.domain.sign.PlotSignType;
import pl.riftplots.storage.repository.PlotSignRepository;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class YamlPlotSignRepository implements PlotSignRepository {
    private final File signsFile;
    private final Map<String, PlotSign> signsCache = new HashMap<>();

    public YamlPlotSignRepository(File signsFile) {
        this.signsFile = signsFile;
        load();
    }

    private synchronized void load() {
        signsCache.clear();
        if (!signsFile.exists()) {
            return;
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(signsFile);
        ConfigurationSection section = config.getConfigurationSection("signs");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            String plotId = section.getString(key + ".plotId");
            String typeStr = section.getString(key + ".type", "FOR_SALE");
            String world = section.getString(key + ".world");
            int x = section.getInt(key + ".x");
            int y = section.getInt(key + ".y");
            int z = section.getInt(key + ".z");

            if (plotId != null && world != null) {
                PlotSign sign = new PlotSign(key, plotId, PlotSignType.parse(typeStr), world, x, y, z);
                signsCache.put(key, sign);
            }
        }
    }

    private synchronized void persist() {
        YamlConfiguration config = new YamlConfiguration();
        for (PlotSign sign : signsCache.values()) {
            String path = "signs." + sign.getId();
            config.set(path + ".plotId", sign.getPlotId());
            config.set(path + ".type", sign.getType().name());
            config.set(path + ".world", sign.getWorldName());
            config.set(path + ".x", sign.getX());
            config.set(path + ".y", sign.getY());
            config.set(path + ".z", sign.getZ());
        }
        try {
            File parent = signsFile.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            config.save(signsFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public synchronized List<PlotSign> findAll() {
        return new ArrayList<>(signsCache.values());
    }

    @Override
    public synchronized Optional<PlotSign> findById(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(signsCache.get(id));
    }

    @Override
    public synchronized Optional<PlotSign> findByLocation(String worldName, int x, int y, int z) {
        if (worldName == null) {
            return Optional.empty();
        }
        return signsCache.values().stream()
                .filter(s -> s.getWorldName().equalsIgnoreCase(worldName) && s.getX() == x && s.getY() == y && s.getZ() == z)
                .findFirst();
    }

    @Override
    public synchronized List<PlotSign> findByPlotId(String plotId) {
        if (plotId == null) {
            return Collections.emptyList();
        }
        return signsCache.values().stream()
                .filter(s -> s.getPlotId().equalsIgnoreCase(plotId))
                .toList();
    }

    @Override
    public synchronized void save(PlotSign sign) {
        if (sign == null) {
            return;
        }
        signsCache.put(sign.getId(), sign);
        persist();
    }

    @Override
    public synchronized void delete(String id) {
        if (id == null) {
            return;
        }
        if (signsCache.remove(id) != null) {
            persist();
        }
    }

    @Override
    public synchronized void deleteByPlotId(String plotId) {
        if (plotId == null) {
            return;
        }
        boolean removed = signsCache.values().removeIf(s -> s.getPlotId().equalsIgnoreCase(plotId));
        if (removed) {
            persist();
        }
    }
}
