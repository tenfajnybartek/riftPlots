package pl.riftplots.domain.sign;

import java.util.Objects;

public class PlotSign {
    private final String id;
    private final String plotId;
    private PlotSignType type;
    private final String worldName;
    private final int x;
    private final int y;
    private final int z;

    public PlotSign(String id, String plotId, PlotSignType type, String worldName, int x, int y, int z) {
        this.id = Objects.requireNonNull(id, "id cannot be null");
        this.plotId = Objects.requireNonNull(plotId, "plotId cannot be null").toLowerCase();
        this.type = type != null ? type : PlotSignType.FOR_SALE;
        this.worldName = Objects.requireNonNull(worldName, "worldName cannot be null");
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static String buildLocationKey(String worldName, int x, int y, int z) {
        return worldName.toLowerCase() + ":" + x + "," + y + "," + z;
    }

    public String getLocationKey() {
        return buildLocationKey(worldName, x, y, z);
    }

    public String getId() {
        return id;
    }

    public String getPlotId() {
        return plotId;
    }

    public PlotSignType getType() {
        return type;
    }

    public void setType(PlotSignType type) {
        this.type = type != null ? type : PlotSignType.FOR_SALE;
    }

    public String getWorldName() {
        return worldName;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlotSign plotSign = (PlotSign) o;
        return x == plotSign.x && y == plotSign.y && z == plotSign.z && worldName.equalsIgnoreCase(plotSign.worldName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(worldName.toLowerCase(), x, y, z);
    }
}
