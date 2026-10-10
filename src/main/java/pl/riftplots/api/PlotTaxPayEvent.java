package pl.riftplots.api;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import pl.riftplots.domain.plot.Plot;

import java.math.BigDecimal;

public class PlotTaxPayEvent extends Event implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private boolean cancelled;
    private final Plot plot;
    private final Player player;
    private BigDecimal amount;
    private final int periods;

    public PlotTaxPayEvent(Plot plot, Player player, BigDecimal amount, int periods) {
        this.plot = plot;
        this.player = player;
        this.amount = amount;
        this.periods = periods;
    }

    public Plot getPlot() {
        return plot;
    }

    public Player getPlayer() {
        return player;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getPeriods() {
        return periods;
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
