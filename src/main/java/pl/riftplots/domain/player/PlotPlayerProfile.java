package pl.riftplots.domain.player;

import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlotPlayerProfile {
    private final UUID uuid;
    private String lastName;
    private final Instant firstJoin;
    private Instant lastSeen;
    private final Set<String> ownedPlotIds = ConcurrentHashMap.newKeySet();

    public PlotPlayerProfile(UUID uuid, String lastName, Instant firstJoin, Instant lastSeen, Set<String> ownedPlots) {
        this.uuid = Objects.requireNonNull(uuid, "uuid cannot be null");
        this.lastName = lastName != null ? lastName : "Unknown";
        this.firstJoin = firstJoin != null ? firstJoin : Instant.now();
        this.lastSeen = lastSeen != null ? lastSeen : Instant.now();
        if (ownedPlots != null) {
            this.ownedPlotIds.addAll(ownedPlots);
        }
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Instant getFirstJoin() {
        return firstJoin;
    }

    public Instant getLastSeen() {
        return lastSeen;
    }

    public void setLastSeen(Instant lastSeen) {
        this.lastSeen = lastSeen;
    }

    public Set<String> getOwnedPlotIds() {
        return Collections.unmodifiableSet(ownedPlotIds);
    }

    public void addOwnedPlot(String plotId) {
        if (plotId != null) {
            ownedPlotIds.add(plotId.toLowerCase());
        }
    }

    public void removeOwnedPlot(String plotId) {
        if (plotId != null) {
            ownedPlotIds.remove(plotId.toLowerCase());
        }
    }
}
