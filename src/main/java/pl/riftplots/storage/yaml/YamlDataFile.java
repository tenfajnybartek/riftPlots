package pl.riftplots.storage.yaml;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

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
            throw new IllegalStateException(pl.riftplots.config.Texts.raw("text.yaml-data-file.nie-mozna-odczytac-danych", file), exception);
        }
    }

    /**
     * Writes the complete replacement before touching an existing data file.
     */
    public static void save(YamlConfiguration yaml, File file) throws IOException {
        Path target = file.toPath().toAbsolutePath();
        Files.createDirectories(target.getParent());
        Path temporary = Files.createTempFile(target.getParent(), "riftplots-", ".tmp");
        try {
            yaml.save(temporary.toFile());
            try {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }
}
