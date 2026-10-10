package pl.riftplots.util;

import pl.riftplots.domain.plot.PlotBounds;

public final class BoundingBoxUtil {
    private BoundingBoxUtil() {
    }

    public static boolean isAdjacentOrTouching(PlotBounds a, PlotBounds b) {
        if (a == null || b == null || !a.getWorldName().equalsIgnoreCase(b.getWorldName())) {
            return false;
        }
        boolean xClose = Math.abs(a.getMinX() - b.getMaxX()) <= 1 || Math.abs(a.getMaxX() - b.getMinX()) <= 1;
        boolean zClose = Math.abs(a.getMinZ() - b.getMaxZ()) <= 1 || Math.abs(a.getMaxZ() - b.getMinZ()) <= 1;
        return xClose || zClose;
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
