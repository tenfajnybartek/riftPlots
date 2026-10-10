package pl.riftplots.api;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotStatus;

public class PlotStateChangeEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final Plot plot;
    private final PlotStatus oldStatus;
    private PlotStatus newStatus;

    public PlotStateChangeEvent(Plot plot, PlotStatus oldStatus, PlotStatus newStatus) {
        this.plot = plot;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
    }

    public Plot getPlot() {
        return plot;
    }

    public PlotStatus getOldStatus() {
        return oldStatus;
    }

    public PlotStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(PlotStatus newStatus) {
        this.newStatus = newStatus;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
