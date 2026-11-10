package pl.riftplots.config;

import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import pl.riftplots.util.AdventureUtil;

import java.util.HashMap;
import java.util.Map;

public class MessageConfiguration {
    private volatile Map<String, String> messages = Map.of();
    private final PluginConfiguration pluginConfig;
    private final String fallbackPrefix;

    public MessageConfiguration(PluginConfiguration pluginConfig) {
        this.pluginConfig = pluginConfig;
        fallbackPrefix = ConfigurationFiles.defaults("config.yml").getString("general.prefix", "");
        load(null);
    }

    public void load(FileConfiguration config) {
        Map<String, String> loaded = new HashMap<>();
        var defaults = ConfigurationFiles.defaults("lang.yml");
        if (config != null) for (String key : defaults.getKeys(true)) {
            if (defaults.isString(key) && config.contains(key) && !config.isString(key))
                throw new IllegalArgumentException("lang.yml: " + key);
        }
        var merged = ConfigurationFiles.merged("lang.yml", config);
        for (String key : merged.getKeys(true)) {
            if (merged.isString(key)) {
                loaded.put(key, merged.getString(key));
            }
        }
        messages = Map.copyOf(loaded);
    }

    public boolean contains(String key) {
        return messages.containsKey(key);
    }

    public String getRaw(String key) {
        return messages.getOrDefault(key, "[" + key + "]");
    }

    public String format(String key, boolean escape, Object... arguments) {
        String value = getRaw(key);
        value = value.replace("{prefix}", pluginConfig != null ? pluginConfig.getPrefix() : fallbackPrefix);
        // Replace in one pass: values never become additional placeholders or formatting instructions.
        var matcher = java.util.regex.Pattern.compile("\\{arg([1-9][0-9]*)}").matcher(value);
        var result = new StringBuilder();
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1)) - 1;
            String replacement = index < arguments.length ? Texts.label(arguments[index]) : matcher.group();
            if (escape)
                replacement = net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().escapeTags(replacement);
            matcher.appendReplacement(result, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    public String getRaw(String key, String fallback) {
        return messages.getOrDefault(key, fallback);
    }

    public Component get(String key, String fallback, Map<String, String> placeholders) {
        String template = getRaw(key, fallback);
        String prefix = pluginConfig != null ? pluginConfig.getPrefix() : fallbackPrefix;
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
        return get(key, "[" + key + "]", Map.of());
    }

    public Component get(String key, Map<String, String> placeholders) {
        return get(key, "[" + key + "]", placeholders);
    }
}
