package pl.riftplots.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotMember;
import pl.riftplots.util.AdventureUtil;
import pl.riftplots.util.TimeUtil;

import java.util.ArrayList;
import java.util.List;

public class PlotMembersGUI {

    public void open(Player player, Plot plot) {
        PlotGUIHolder holder = new PlotGUIHolder("PLOT_MEMBERS", plot.getId());
        Inventory inv = Bukkit.createInventory(holder, 45, AdventureUtil.parse("<aqua><b>Członkowie: " + plot.getName() + "</b></aqua>"));
        holder.setInventory(inv);

        int slot = 0;
        for (PlotMember member : plot.getMembers().values()) {
            if (slot >= 36) {
                break;
            }
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            ItemMeta meta = head.getItemMeta();
            meta.displayName(AdventureUtil.parse("<aqua><b>" + member.getPlayerName() + "</b></aqua>"));
            List<Component> lore = new ArrayList<>();
            lore.add(AdventureUtil.parse("<gray>Rola: </gray><yellow>" + member.getRole() + "</yellow>"));
            lore.add(AdventureUtil.parse("<gray>Dodano: </gray><white>" + TimeUtil.formatInstant(member.getAddedAt()) + "</white>"));
            lore.add(AdventureUtil.parse("<gray>Uprawnienia: </gray><gold>" + member.getPermissions().size() + "</gold>"));
            lore.add(Component.empty());
            lore.add(AdventureUtil.parse("<red>Kliknij, aby usunąć członka!</red>"));
            meta.lore(lore);
            head.setItemMeta(meta);
            inv.setItem(slot++, head);
        }

        ItemStack backItem = new ItemStack(Material.ARROW);
        ItemMeta backMeta = backItem.getItemMeta();
        backMeta.displayName(AdventureUtil.parse("<yellow><b>Powrót</b></yellow>"));
        backItem.setItemMeta(backMeta);
        inv.setItem(40, backItem);

        player.openInventory(inv);
    }
}
