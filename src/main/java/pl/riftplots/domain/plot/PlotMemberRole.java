package pl.riftplots.domain.plot;

public enum PlotMemberRole {
    MEMBER,
    TENANT,
    MANAGER;

    public static PlotMemberRole parse(String input) {
        if (input == null || input.isBlank()) {
            return MEMBER;
        }
        try {
            return PlotMemberRole.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return MEMBER;
        }
    }
}
