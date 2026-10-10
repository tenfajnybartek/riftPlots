package pl.riftplots.config;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import pl.riftplots.util.AdventureUtil;

import java.util.HashMap;
import java.util.Map;

public class MessageConfiguration {
    private final Map<String, String> messages = new HashMap<>();
    private final PluginConfiguration pluginConfig;

    public MessageConfiguration(PluginConfiguration pluginConfig) {
        this.pluginConfig = pluginConfig;
    }

    public void load(FileConfiguration config) {
        messages.clear();
        for (String key : config.getKeys(true)) {
            if (config.isString(key)) {
                messages.put(key, config.getString(key));
            }
        }
    }

    public String getRaw(String key, String fallback) {
        return messages.getOrDefault(key, fallback);
    }

    public Component get(String key, String fallback, Map<String, String> placeholders) {
        String template = getRaw(key, fallback);
        String prefix = pluginConfig.getPrefix();
        template = template.replace("{prefix}", prefix);

        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                template = template.replace("{" + entry.getKey() + "}", entry.getValue() != null ? entry.getValue() : "");
            }
        }
        return AdventureUtil.parse(template);
    }

    public Component get(String key, String fallback) {
        return get(key, fallback, Map.of());
    }

    public Component get(String key) {
        return get(key, "<red>Brak wiadomości w lang.yml: " + key + "</red>", Map.of());
    }

    public Component get(String key, Map<String, String> placeholders) {
        return get(key, "<red>Brak wiadomości w lang.yml: " + key + "</red>", placeholders);
    }
}
