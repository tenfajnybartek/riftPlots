package pl.riftplots.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

public final class GuiData {
    private GuiData() {
    }

    private static NamespacedKey key(String key) {
        return new NamespacedKey("riftplots", key);
    }

    public static void set(ItemStack item, String key, String value) {
        var meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(key(key), PersistentDataType.STRING, value);
        item.setItemMeta(meta);
    }

    public static String get(ItemStack item, String key) {
        return item != null && item.hasItemMeta() ? item.getItemMeta().getPersistentDataContainer().get(key(key), PersistentDataType.STRING) : null;
    }

    public static ItemStack item(Material material, String title, String... description) {
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(title, NamedTextColor.AQUA));
        meta.lore(java.util.Arrays.stream(description).map(line -> Component.text(line, NamedTextColor.GRAY)).toList());
        item.setItemMeta(meta);
        return item;
    }
}
