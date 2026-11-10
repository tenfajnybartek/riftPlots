package pl.riftplots.service.io;

import java.util.concurrent.atomic.AtomicBoolean;

public final class MaintenanceGate {
    private final AtomicBoolean active = new AtomicBoolean();

    public boolean isActive() {
        return active.get();
    }

    public void enter() {
        if (!active.compareAndSet(false, true)) throw new IllegalStateException("Trwa inna operacja utrzymaniowa.");
    }

    public void leave() {
        active.set(false);
    }
}
