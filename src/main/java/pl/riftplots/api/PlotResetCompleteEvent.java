package pl.riftplots.api;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import pl.riftplots.domain.plot.Plot;

public class PlotResetCompleteEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Plot plot;
    private final boolean successful;

    public PlotResetCompleteEvent(Plot plot, boolean successful) {
        this.plot = plot;
        this.successful = successful;
    }

    public Plot getPlot() {
        return plot;
    }

    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
