package pl.riftplots;

import org.bukkit.configuration.file.YamlConfiguration;
import pl.riftplots.config.MessageConfiguration;
import pl.riftplots.config.PluginConfiguration;
import pl.riftplots.domain.plot.*;

import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

final class TestFixtures {
    static Plot plot(UUID owner) {
        return new Plot("test_plot", "Test", "", PlotCategory.RESIDENTIAL,
                owner == null ? PlotStatus.AVAILABLE : PlotStatus.OWNED,
                new PlotBounds("world", 0, 60, 0, 10, 80, 10), "riftplot_test_plot",
                new BigDecimal("1000"), new BigDecimal("100"), TaxMode.FIXED, 1, true, 7, 6,
                owner, owner == null ? null : "Owner", List.of(), null, null, null,
                0, "test_plot.schem", true, 50, Instant.now(), Instant.now());
    }

    static YamlConfiguration resource(String name) {
        try (var reader = new InputStreamReader(TestFixtures.class.getResourceAsStream("/" + name), StandardCharsets.UTF_8)) {
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.load(reader);
            return yaml;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static PluginConfiguration config() {
        PluginConfiguration config = new PluginConfiguration();
        config.load(resource("config.yml"));
        return config;
    }

    static MessageConfiguration messages(PluginConfiguration config) {
        MessageConfiguration messages = new MessageConfiguration(config);
        messages.load(resource("lang.yml"));
        return messages;
    }
}
