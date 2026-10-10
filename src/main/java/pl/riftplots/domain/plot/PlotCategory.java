package pl.riftplots.domain.plot;

public enum PlotCategory {
    RESIDENTIAL,
    COMMERCIAL,
    INDUSTRIAL,
    PUBLIC,
    CUSTOM;

    public static PlotCategory parse(String input) {
        if (input == null || input.isBlank()) {
            return RESIDENTIAL;
        }
        try {
            return PlotCategory.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return CUSTOM;
        }
    }
}
