package pl.riftplots.config;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.charset.StandardCharsets;

/**
 * Bundled YAML is the sole source of presentation and policy defaults.
 */
public final class ConfigurationFiles {
    private ConfigurationFiles() {
    }

    public static YamlConfiguration defaults(String resource) {
        try (var stream = ConfigurationFiles.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) throw new IllegalStateException(resource);
            var yaml = new YamlConfiguration();
            yaml.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return yaml;
        } catch (Exception e) {
            throw new IllegalStateException(resource, e);
        }
    }

    public static YamlConfiguration merged(String resource, org.bukkit.configuration.ConfigurationSection overrides) {
        var result = defaults(resource);
        if (overrides != null) for (String key : overrides.getKeys(true)) {
            Object expected = result.get(key), actual = overrides.get(key);
            if (expected != null && actual != null) {
                boolean compatible = expected instanceof org.bukkit.configuration.ConfigurationSection
                        ? actual instanceof org.bukkit.configuration.ConfigurationSection
                        : expected instanceof Number ? actual instanceof Number
                        : expected instanceof java.util.List<?> ? actual instanceof java.util.List<?>
                        : expected.getClass().isInstance(actual);
                if (!compatible) throw new IllegalArgumentException(resource + ": " + key);
            }
            if (!overrides.isConfigurationSection(key)) result.set(key, overrides.get(key));
        }
        return result;
    }

    public static YamlConfiguration read(File file) {
        if (!file.isFile()) throw new IllegalArgumentException(file.toString());
        try {
            var result = new YamlConfiguration();
            result.load(file);
            return result;
        } catch (Exception e) {
            throw new IllegalArgumentException(file.toString(), e);
        }
    }
}
