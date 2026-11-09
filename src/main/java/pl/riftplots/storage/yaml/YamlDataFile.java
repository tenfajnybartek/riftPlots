package pl.riftplots.storage.yaml;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

/**
 * Data files must fail visibly instead of being replaced with empty defaults.
 */
public final class YamlDataFile {
    private YamlDataFile() {
    }

    public static YamlConfiguration load(File file) {
        YamlConfiguration yaml = new YamlConfiguration();
        if (!file.exists()) return yaml;
        try {
            yaml.load(file);
            return yaml;
        } catch (IOException | InvalidConfigurationException exception) {
            throw new IllegalStateException("Nie można odczytać danych: " + file, exception);
        }
    }
}
