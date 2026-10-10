package pl.riftplots.domain.plot;

public enum PlotPermission {
    BUILD,
    DESTROY,
    INTERACT,
    CONTAINERS,
    DOORS,
    VEHICLES,
    TELEPORT;

    public static PlotPermission parse(String input) {
        if (input == null || input.isBlank()) {
            return null;
        }
        try {
            return PlotPermission.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
