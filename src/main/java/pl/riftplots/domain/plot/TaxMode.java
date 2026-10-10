package pl.riftplots.domain.plot;

public enum TaxMode {
    FIXED,
    PER_BLOCK,
    PER_CHUNK,
    SIZE_TIERS,
    MULTIPLIER,
    CUSTOM;

    public static TaxMode parse(String input) {
        if (input == null || input.isBlank()) {
            return FIXED;
        }
        try {
            return TaxMode.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FIXED;
        }
    }
}
