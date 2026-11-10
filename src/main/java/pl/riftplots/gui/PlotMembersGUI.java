package pl.riftplots.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotMember;
import pl.riftplots.util.AdventureUtil;

import java.util.Comparator;

public class PlotMembersGUI {
    public void open(Player player, Plot plot) {
        open(player, plot, 0);
    }

    public void open(Player player, Plot plot, int requestedPage) {
        var members = plot.getMembers().values().stream().sorted(Comparator.comparing(PlotMember::getPlayerName)).toList();
        int page = Math.clamp(requestedPage, 0, Math.max(0, (members.size() - 1) / 36));
        var holder = new PlotGUIHolder("PLOT_MEMBERS", plot.getId());
        holder.setPage(page);
        var inventory = Bukkit.createInventory(holder, 45, AdventureUtil.parse("<aqua>Członkowie · " + (page + 1) + "</aqua>"));
        holder.setInventory(inventory);
        var shown = members.stream().skip((long) page * 36).limit(36).toList();
        for (int i = 0; i < shown.size(); i++) {
            var member = shown.get(i);
            var item = GuiData.item(Material.PLAYER_HEAD, member.getPlayerName(), "Rola: " + member.getRole(),
                    "Lewy przycisk: edytor dostępu", "Prawy przycisk: usuń członka", "Aktywny najem kończy się przez umowę.");
            GuiData.set(item, "member", member.getPlayerUuid().toString());
            inventory.setItem(i, item);
        }
        if (page > 0) inventory.setItem(36, GuiData.item(Material.ARROW, "Poprzednia strona"));
        if ((page + 1L) * 36 < members.size()) inventory.setItem(44, GuiData.item(Material.ARROW, "Następna strona"));
        inventory.setItem(40, GuiData.item(Material.ARROW, "Powrót"));
        player.openInventory(inventory);
    }
}
