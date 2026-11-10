package pl.riftplots.gui;

import org.bukkit.entity.Player;
import pl.riftplots.config.*;
import pl.riftplots.domain.plot.*;

import java.util.*;

public class PlotMembersGUI {
    public void open(Player player, Plot plot) {
        open(player, plot, 0);
    }

    public void open(Player player, Plot plot, int requestedPage) {
        var gui = Presentation.gui();
        var slots = gui.slots("members");
        var members = plot.getMembers().values().stream().sorted(Comparator.comparing(PlotMember::getPlayerName)).toList();
        int page = Math.clamp(requestedPage, 0, Math.max(0, (members.size() - 1) / slots.size()));
        var holder = new PlotGUIHolder("PLOT_MEMBERS", plot.getId());
        holder.setPage(page);
        var variables = new HashMap<String, Object>();
        variables.put("page", page + 1);
        variables.put("plot_id", plot.getId());
        var inventory = gui.create("members", holder, variables);
        var shown = members.stream().skip((long) page * slots.size()).limit(slots.size()).toList();
        for (int index = 0; index < shown.size(); index++) {
            var member = shown.get(index);
            variables.put("member", member.getPlayerName());
            variables.put("role", Texts.label(member.getRole()));
            var item = gui.entry("members", variables);
            GuiData.set(item, "member", member.getPlayerUuid().toString());
            GuiData.set(item, "action", "member-entry");
            inventory.setItem(slots.get(index), item);
        }
        Set<String> hidden = new HashSet<>();
        if (page == 0) hidden.add("previous");
        if ((page + 1L) * slots.size() >= members.size()) hidden.add("next");
        gui.buttons(inventory, "members", variables, hidden);
        player.openInventory(inventory);
    }
}
