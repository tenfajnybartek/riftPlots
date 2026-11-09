package pl.riftplots.util;

import pl.riftplots.domain.plot.PlotBounds;

public final class BoundingBoxUtil {
    private BoundingBoxUtil() {
    }

    public static boolean isAdjacentOrTouching(PlotBounds a, PlotBounds b) {
        if (a == null || b == null || !a.getWorldName().equalsIgnoreCase(b.getWorldName())) {
            return false;
        }
        return a.getMinX() <= (long) b.getMaxX() + 1 && b.getMinX() <= (long) a.getMaxX() + 1
                && a.getMinY() <= (long) b.getMaxY() + 1 && b.getMinY() <= (long) a.getMaxY() + 1
                && a.getMinZ() <= (long) b.getMaxZ() + 1 && b.getMinZ() <= (long) a.getMaxZ() + 1;
    }

    public static boolean isLocationOnBorder(PlotBounds bounds, int x, int y, int z) {
        if (bounds == null) {
            return false;
        }
        if (x < bounds.getMinX() || x > bounds.getMaxX()
                || y < bounds.getMinY() || y > bounds.getMaxY()
                || z < bounds.getMinZ() || z > bounds.getMaxZ()) {
            return false;
        }
        return x == bounds.getMinX() || x == bounds.getMaxX()
                || z == bounds.getMinZ() || z == bounds.getMaxZ();
    }
}
