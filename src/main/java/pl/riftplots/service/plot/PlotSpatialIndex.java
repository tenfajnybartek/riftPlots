package pl.riftplots.service.plot;

import pl.riftplots.domain.plot.PlotBounds;

import java.util.*;

/**
 * A chunk only narrows the candidates; exact 3D bounds still decide ownership.
 */
public final class PlotSpatialIndex {
    private record Chunk(String world, int x, int z) {
        private Chunk {
            world = world.toLowerCase(Locale.ROOT);
        }
    }

    private final Map<Chunk, Set<String>> chunks = new HashMap<>();
    private final Map<String, PlotBounds> bounds = new HashMap<>();
    private final Set<String> large = new HashSet<>();

    public synchronized void clear() {
        chunks.clear();
        bounds.clear();
        large.clear();
    }

    public synchronized void put(String id, PlotBounds area) {
        remove(id);
        bounds.put(id, area);
        long count = ((long) (area.getMaxX() >> 4) - (area.getMinX() >> 4) + 1)
                * ((long) (area.getMaxZ() >> 4) - (area.getMinZ() >> 4) + 1);
        if (count > pl.riftplots.config.Settings.integer("performance.spatial-index-max-chunks-per-plot")) {
            large.add(id);
            return;
        }
        for (int x = area.getMinX() >> 4; x <= area.getMaxX() >> 4; x++) {
            for (int z = area.getMinZ() >> 4; z <= area.getMaxZ() >> 4; z++) {
                chunks.computeIfAbsent(new Chunk(area.getWorldName(), x, z), key -> new HashSet<>()).add(id);
            }
        }
    }

    public synchronized void remove(String id) {
        PlotBounds old = bounds.remove(id);
        if (old == null || large.remove(id)) return;
        for (int x = old.getMinX() >> 4; x <= old.getMaxX() >> 4; x++) {
            for (int z = old.getMinZ() >> 4; z <= old.getMaxZ() >> 4; z++) {
                Chunk key = new Chunk(old.getWorldName(), x, z);
                Set<String> ids = chunks.get(key);
                if (ids != null && ids.remove(id) && ids.isEmpty()) chunks.remove(key);
            }
        }
    }

    public synchronized Set<String> candidates(String world, int x, int z) {
        Set<String> ids = new HashSet<>(chunks.getOrDefault(new Chunk(world, x >> 4, z >> 4), Set.of()));
        for (String id : large) if (bounds.get(id).getWorldName().equalsIgnoreCase(world)) ids.add(id);
        return ids;
    }
}
