package pl.riftplots.config;

public final class Presentation {
    private static volatile GuiConfiguration gui = new GuiConfiguration();

    private Presentation() {
    }

    public static GuiConfiguration gui() {
        return gui;
    }

    public static void install(GuiConfiguration configuration) {
        gui = java.util.Objects.requireNonNull(configuration);
    }
}
