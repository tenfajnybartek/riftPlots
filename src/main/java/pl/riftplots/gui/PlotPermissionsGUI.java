package pl.riftplots.gui;

import org.bukkit.entity.Player;
import pl.riftplots.config.*;
import pl.riftplots.domain.plot.*;

import java.util.*;

public final class PlotPermissionsGUI {
    public void open(Player player, Plot plot, PlotMember member) {
        var gui = Presentation.gui();
        var slots = gui.slots("permissions");
        var holder = new PlotGUIHolder("PLOT_PERMISSIONS", plot.getId());
        holder.setMember(member.getPlayerUuid());
        Map<String, Object> variables = new HashMap<>();
        variables.put("member", member.getPlayerName());
        variables.put("member_role", Texts.label(PlotMemberRole.MEMBER));
        variables.put("manager_role", Texts.label(PlotMemberRole.MANAGER));
        var inventory = gui.create("permissions", holder, variables);
        int index = 0;
        for (PlotPermission permission : PlotPermission.values()) {
            variables.put("permission", Texts.label(permission));
            variables.put("allowed", Texts.label(member.hasPermission(permission)));
            variables.put("permission_icon", gui.value(member.hasPermission(permission) ? "permissions.allowed-material" : "permissions.denied-material"));
            var item = gui.entry("permissions", variables);
            GuiData.set(item, "permission", permission.name());
            GuiData.set(item, "action", "permission-entry");
            inventory.setItem(slots.get(index++), item);
        }
        gui.buttons(inventory, "permissions", variables, Set.of());
        player.openInventory(inventory);
    }
}
