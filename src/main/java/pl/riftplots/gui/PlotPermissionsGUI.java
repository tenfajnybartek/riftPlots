package pl.riftplots.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.riftplots.domain.plot.*;
import pl.riftplots.util.AdventureUtil;

public final class PlotPermissionsGUI {
    public void open(Player player, Plot plot, PlotMember member) {
        var holder = new PlotGUIHolder("PLOT_PERMISSIONS", plot.getId());
        holder.setMember(member.getPlayerUuid());
        var inventory = Bukkit.createInventory(holder, 36, AdventureUtil.parse("<aqua>Dostęp: " + member.getPlayerName() + "</aqua>"));
        holder.setInventory(inventory);
        int slot = 10;
        for (PlotPermission permission : PlotPermission.values()) {
            var item = GuiData.item(member.hasPermission(permission) ? Material.LIME_DYE : Material.GRAY_DYE,
                    permission.name() + ": " + member.hasPermission(permission), "Kliknij, aby zmienić.");
            GuiData.set(item, "permission", permission.name());
            inventory.setItem(slot++, item);
        }
        for (PlotMemberRole role : new PlotMemberRole[]{PlotMemberRole.MEMBER, PlotMemberRole.MANAGER}) {
            var item = GuiData.item(Material.NAME_TAG, "Rola: " + role, "MANAGER otrzymuje wszystkie prawa działki.");
            GuiData.set(item, "role", role.name());
            inventory.setItem(role == PlotMemberRole.MEMBER ? 27 : 28, item);
        }
        inventory.setItem(31, GuiData.item(Material.ARROW, "Powrót"));
        player.openInventory(inventory);
    }
}
