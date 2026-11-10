package pl.riftplots.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

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

}
