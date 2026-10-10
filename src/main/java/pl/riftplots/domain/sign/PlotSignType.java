package pl.riftplots.domain.sign;

public enum PlotSignType {
    FOR_SALE,
    STATUS,
    RENEW;

    public static PlotSignType parse(String input) {
        if (input == null || input.isBlank()) {
            return FOR_SALE;
        }
        try {
            return PlotSignType.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FOR_SALE;
        }
    }
}
