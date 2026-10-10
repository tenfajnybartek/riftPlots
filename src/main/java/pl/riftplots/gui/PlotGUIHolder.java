package pl.riftplots.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PlotGUIHolder implements InventoryHolder {
    private final String guiType;
    private final String plotId;
    private Inventory inventory;

    public PlotGUIHolder(String guiType, String plotId) {
        this.guiType = guiType;
        this.plotId = plotId;
    }

    public String getGuiType() {
        return guiType;
    }

    public String getPlotId() {
        return plotId;
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }
}
