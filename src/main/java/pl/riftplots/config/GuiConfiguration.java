package pl.riftplots.config;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import pl.riftplots.gui.GuiData;
import pl.riftplots.gui.PlotGUIHolder;

import java.util.*;
import java.util.regex.*;

public final class GuiConfiguration {
    private volatile YamlConfiguration settings;
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    public GuiConfiguration() {
        load(ConfigurationFiles.defaults("gui.yml"));
    }

    public synchronized void load(org.bukkit.configuration.ConfigurationSection custom) {
        var candidate = ConfigurationFiles.merged("gui.yml", custom);
        if (candidate.getInt("format") != 1) throw new IllegalArgumentException("gui.yml: format");
        pl.riftplots.gui.MarketView.Tab.valueOf(candidate.getString("menus.market.default-tab"));
        pl.riftplots.gui.MarketView.Sort.valueOf(candidate.getString("menus.market.default-sort"));
        var menus = candidate.getConfigurationSection("menus");
        for (String name : menus.getKeys(false)) {
            String base = "menus." + name;
            int size = candidate.getInt(base + ".size");
            if (size < 9 || size > 54 || size % 9 != 0) throw new IllegalArgumentException(base + ".size");
            Set<Integer> occupied = new HashSet<>();
            var buttons = candidate.getConfigurationSection(base + ".buttons");
            if (buttons != null) for (String button : buttons.getKeys(false)) {
                String path = base + ".buttons." + button;
                if (!candidate.getBoolean(path + ".enabled", true)) continue;
                int slot = candidate.getInt(path + ".slot", -1);
                if (slot < 0 || slot >= size || !occupied.add(slot)) throw new IllegalArgumentException(path + ".slot");
            }
            if (candidate.contains(base + ".content-slots")) {
                if (candidate.getList(base + ".content-slots").stream().anyMatch(slot -> !(slot instanceof Integer)))
                    throw new IllegalArgumentException(base + ".content-slots");
                List<Integer> content = candidate.getIntegerList(base + ".content-slots");
                if (content.isEmpty()) throw new IllegalArgumentException(base + ".content-slots");
                for (int slot : content)
                    if (slot < 0 || slot >= size || !occupied.add(slot))
                        throw new IllegalArgumentException(base + ".content-slots");
            }
        }
        for (String key : candidate.getKeys(true)) {
            if (key.endsWith(".lore") && (!candidate.isList(key) || candidate.getList(key).stream().anyMatch(line -> !(line instanceof String))))
                throw new IllegalArgumentException(key);
            if (key.endsWith(".material") || key.endsWith("-material")) {
                String value = candidate.getString(key, "");
                if (!value.contains("{") && (Material.matchMaterial(value) == null || Material.matchMaterial(value) == Material.AIR
                        || Bukkit.getServer() != null && !Material.matchMaterial(value).isItem()))
                    throw new IllegalArgumentException(key);
            }
            if (candidate.isString(key) && (key.endsWith(".name") || key.endsWith(".title")))
                MINI.deserialize(candidate.getString(key));
        }
        if (candidate.getIntegerList("menus.permissions.content-slots").size() < pl.riftplots.domain.plot.PlotPermission.values().length)
            throw new IllegalArgumentException("menus.permissions.content-slots");
        if (candidate.getInt("menus.auction.history-lines") < 0 || candidate.getInt("menus.auction.history-lines") > 100)
            throw new IllegalArgumentException("menus.auction.history-lines");
        settings = candidate;
    }

    public List<Integer> slots(String menu) {
        return List.copyOf(settings.getIntegerList("menus." + menu + ".content-slots"));
    }

    public int size(String menu) {
        return settings.getInt("menus." + menu + ".size");
    }

    public int number(String path) {
        return settings.getInt("menus." + path);
    }

    public String value(String path) {
        return settings.getString("menus." + path, "");
    }

    public boolean enabled(String menu, String id) {
        return settings.getBoolean("menus." + menu + ".buttons." + id + ".enabled", true);
    }

    public String actionAt(String menu, int slot) {
        var buttons = settings.getConfigurationSection("menus." + menu + ".buttons");
        if (buttons != null) for (String id : buttons.getKeys(false))
            if (enabled(menu, id) && buttons.getInt(id + ".slot") == slot) return id;
        return null;
    }

    public Component render(String template, Map<String, ?> variables) {
        var matcher = Pattern.compile("\\{([a-zA-Z0-9_-]+)}").matcher(template);
        var result = new StringBuilder();
        while (matcher.find()) {
            Object value = variables.get(matcher.group(1));
            String replacement = value instanceof Component component ? MINI.serialize(component)
                    : MINI.escapeTags(value == null ? matcher.group() : Texts.label(value));
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return MINI.deserialize(result.toString());
    }

    public Inventory create(String menu, PlotGUIHolder holder, Map<String, ?> variables) {
        Inventory inventory = Bukkit.createInventory(holder, size(menu), render(value(menu + ".title"), variables));
        holder.setInventory(inventory);
        String path = "menus." + menu + ".filler";
        if (settings.getBoolean(path + ".enabled")) {
            ItemStack filler = itemAt(path, variables);
            for (int index = 0; index < inventory.getSize(); index++) inventory.setItem(index, filler.clone());
        }
        return inventory;
    }

    public ItemStack entry(String menu, Map<String, ?> variables) {
        return itemAt("menus." + menu + ".entry", variables);
    }

    public ItemStack button(Inventory inventory, String menu, String id, Map<String, ?> variables) {
        String path = "menus." + menu + ".buttons." + id;
        if (!settings.getBoolean(path + ".enabled", true)) return null;
        ItemStack item = itemAt(path, variables);
        GuiData.set(item, "action", id);
        inventory.setItem(settings.getInt(path + ".slot"), item);
        return item;
    }

    public void buttons(Inventory inventory, String menu, Map<String, ?> variables, Set<String> hidden) {
        var section = settings.getConfigurationSection("menus." + menu + ".buttons");
        if (section != null)
            for (String id : section.getKeys(false)) if (!hidden.contains(id)) button(inventory, menu, id, variables);
    }

    private ItemStack itemAt(String path, Map<String, ?> variables) {
        String icon = settings.getString(path + ".material", "");
        for (var variable : variables.entrySet())
            icon = icon.replace("{" + variable.getKey() + "}", Objects.toString(variable.getValue(), ""));
        Material material = Material.matchMaterial(icon);
        if (material == null || !material.isItem()) throw new IllegalArgumentException(path + ".material=" + icon);
        var item = new ItemStack(material, Math.clamp(settings.getInt(path + ".amount", 1), 1, material.getMaxStackSize()));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(render(settings.getString(path + ".name", ""), variables));
        List<Component> lore = new ArrayList<>();
        for (String line : settings.getStringList(path + ".lore")) {
            Object block = line.startsWith("{") && line.endsWith("}") ? variables.get(line.substring(1, line.length() - 1)) : null;
            if (block instanceof List<?> values) for (Object value : values)
                lore.add(value instanceof Component component ? component : Component.text(value.toString()));
            else lore.add(render(line, variables));
        }
        meta.lore(lore);
        if (settings.contains(path + ".glow")) meta.setEnchantmentGlintOverride(settings.getBoolean(path + ".glow"));
        if (settings.contains(path + ".custom-model-data"))
            meta.setCustomModelData(settings.getInt(path + ".custom-model-data"));
        item.setItemMeta(meta);
        return item;
    }
}
