package pl.riftplots.domain.plot;

import java.util.Objects;

public class PlotBounds {
    private final String worldName;
    private final int minX;
    private final int minY;
    private final int minZ;
    private final int maxX;
    private final int maxY;
    private final int maxZ;

    public PlotBounds(String worldName, int x1, int y1, int z1, int x2, int y2, int z2) {
        this.worldName = Objects.requireNonNull(worldName, pl.riftplots.config.Texts.raw("text.plot-bounds.worldname-cannot-be-null"));
        this.minX = Math.min(x1, x2);
        this.minY = Math.min(y1, y2);
        this.minZ = Math.min(z1, z2);
        this.maxX = Math.max(x1, x2);
        this.maxY = Math.max(y1, y2);
        this.maxZ = Math.max(z1, z2);
    }

    public String getWorldName() {
        return worldName;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinY() {
        return minY;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxY() {
        return maxY;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public int getWidthX() {
        return maxX - minX + 1;
    }

    public int getHeightY() {
        return maxY - minY + 1;
    }

    public int getLengthZ() {
        return maxZ - minZ + 1;
    }

    public long getHorizontalArea() {
        return (long) getWidthX() * getLengthZ();
    }

    public long getVolume() {
        return (long) getWidthX() * getHeightY() * getLengthZ();
    }

    public int getChunksCount() {
        int minChunkX = minX >> 4;
        int maxChunkX = maxX >> 4;
        int minChunkZ = minZ >> 4;
        int maxChunkZ = maxZ >> 4;
        return (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1);
    }

    public boolean contains(String targetWorld, int x, int y, int z) {
        if (!this.worldName.equalsIgnoreCase(targetWorld)) {
            return false;
        }
        return x >= minX && x <= maxX
                && y >= minY && y <= maxY
                && z >= minZ && z <= maxZ;
    }

    public boolean containsHorizontal(String targetWorld, int x, int z) {
        if (!this.worldName.equalsIgnoreCase(targetWorld)) {
            return false;
        }
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    public boolean intersects(PlotBounds other) {
        if (other == null || !this.worldName.equalsIgnoreCase(other.worldName)) {
            return false;
        }
        return this.minX <= other.maxX && this.maxX >= other.minX
                && this.minY <= other.maxY && this.maxY >= other.minY
                && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    public boolean intersectsHorizontal(PlotBounds other) {
        if (other == null || !this.worldName.equalsIgnoreCase(other.worldName)) {
            return false;
        }
        return this.minX <= other.maxX && this.maxX >= other.minX
                && this.minZ <= other.maxZ && this.maxZ >= other.minZ;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlotBounds that = (PlotBounds) o;
        return minX == that.minX && minY == that.minY && minZ == that.minZ
                && maxX == that.maxX && maxY == that.maxY && maxZ == that.maxZ
                && worldName.equalsIgnoreCase(that.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName.toLowerCase(), minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public String toString() {
        return worldName + ":" + minX + "," + minY + "," + minZ + "->" + maxX + "," + maxY + "," + maxZ;
    }
}
