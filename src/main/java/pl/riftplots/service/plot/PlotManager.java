package pl.riftplots.service.plot;

import pl.riftplots.domain.plot.Plot;
import pl.riftplots.domain.plot.PlotBounds;
import pl.riftplots.domain.plot.PlotStatus;
import pl.riftplots.integration.worldguard.WorldGuardHook;
import pl.riftplots.storage.repository.PlotRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlotManager {
    private final PlotRepository repository;
    private final WorldGuardHook worldGuardHook;
    private final Map<String, Plot> plots = new ConcurrentHashMap<>();

    public PlotManager(PlotRepository repository, WorldGuardHook worldGuardHook) {
        this.repository = Objects.requireNonNull(repository, "repository cannot be null");
        this.worldGuardHook = Objects.requireNonNull(worldGuardHook, "worldGuardHook cannot be null");
    }

    public void loadAll() {
        plots.clear();
        List<Plot> list = repository.findAll();
        for (Plot plot : list) {
            plots.put(plot.getId(), plot);
        }
    }

    public Collection<Plot> getAllPlots() {
        return Collections.unmodifiableCollection(plots.values());
    }

    public Optional<Plot> getPlot(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(plots.get(id.toLowerCase()));
    }

    public Optional<Plot> getPlotAt(String worldName, int x, int y, int z) {
        if (worldName == null) {
            return Optional.empty();
        }
        for (Plot plot : plots.values()) {
            if (plot.getBounds().contains(worldName, x, y, z)) {
                return Optional.of(plot);
            }
        }
        return Optional.empty();
    }

    public Optional<Plot> getPlotAtHorizontal(String worldName, int x, int z) {
        if (worldName == null) {
            return Optional.empty();
        }
        for (Plot plot : plots.values()) {
            if (plot.getBounds().containsHorizontal(worldName, x, z)) {
                return Optional.of(plot);
            }
        }
        return Optional.empty();
    }

    public List<Plot> getPlotsByOwner(UUID ownerUuid) {
        if (ownerUuid == null) {
            return Collections.emptyList();
        }
        List<Plot> list = new ArrayList<>();
        for (Plot plot : plots.values()) {
            if (ownerUuid.equals(plot.getOwnerUuid())) {
                list.add(plot);
            }
        }
        return list;
    }

    public List<Plot> getPlotsByMember(UUID memberUuid) {
        if (memberUuid == null) {
            return Collections.emptyList();
        }
        List<Plot> list = new ArrayList<>();
        for (Plot plot : plots.values()) {
            if (plot.isMember(memberUuid)) {
                list.add(plot);
            }
        }
        return list;
    }

    public PlotValidationResult validateNewPlot(String id, PlotBounds bounds) {
        if (id == null || id.isBlank()) {
            return PlotValidationResult.error("Identyfikator działki nie może być pusty!");
        }
        String cleanId = id.toLowerCase();
        if (!cleanId.matches("^[a-zA-Z0-9_-]{2,32}$")) {
            return PlotValidationResult.error("Identyfikator może zawierać tylko litery, cyfry, podkreślenia i myślniki (2-32 znaki)!");
        }
        if (plots.containsKey(cleanId)) {
            return PlotValidationResult.error("Działka o identyfikatorze " + cleanId + " już istnieje!");
        }
        if (bounds == null) {
            return PlotValidationResult.error("Granice działki są nieprawidłowe!");
        }

        for (Plot existing : plots.values()) {
            if (existing.getBounds().intersects(bounds)) {
                return PlotValidationResult.error("Kolizja z istniejącą działką: " + existing.getId());
            }
        }

        String conflictRegion = worldGuardHook.findConflictingRegion(bounds, "riftplot_" + cleanId);
        if (conflictRegion != null) {
            return PlotValidationResult.error("Kolizja z istniejącym regionem WorldGuard: " + conflictRegion);
        }

        return PlotValidationResult.ok();
    }

    public boolean registerPlot(Plot plot) {
        if (plot == null) {
            return false;
        }
        plots.put(plot.getId(), plot);
        repository.save(plot);
        worldGuardHook.createOrUpdateRegion(plot);
        return true;
    }

    public void savePlot(Plot plot) {
        if (plot == null) {
            return;
        }
        plot.touch();
        plots.put(plot.getId(), plot);
        repository.save(plot);
        worldGuardHook.syncMembers(plot);
    }

    public boolean deletePlot(String id) {
        if (id == null) {
            return false;
        }
        Plot plot = plots.remove(id.toLowerCase());
        if (plot == null) {
            return false;
        }
        worldGuardHook.removeRegion(plot.getBounds().getWorldName(), plot.getWorldGuardRegionId());
        repository.delete(id.toLowerCase());
        return true;
    }
}
