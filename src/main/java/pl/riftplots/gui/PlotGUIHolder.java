package pl.riftplots.gui;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class PlotGUIHolder implements InventoryHolder {
    private final String guiType;
    private final String plotId;
    private Inventory inventory;
    private MarketView marketView;
    private boolean autoRenew;
    private int page;
    private java.util.UUID member;

    public MarketView getMarketView() {
        return marketView;
    }

    public void setMarketView(MarketView marketView) {
        this.marketView = marketView;
    }

    public boolean isAutoRenew() {
        return autoRenew;
    }

    public void setAutoRenew(boolean autoRenew) {
        this.autoRenew = autoRenew;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = Math.max(0, page);
    }

    public java.util.UUID getMember() {
        return member;
    }

    public void setMember(java.util.UUID member) {
        this.member = member;
    }

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
