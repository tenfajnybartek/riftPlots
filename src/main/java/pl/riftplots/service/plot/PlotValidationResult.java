package pl.riftplots.service.plot;

public class PlotValidationResult {
    private final boolean valid;
    private final String errorMessage;

    private PlotValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    public static PlotValidationResult ok() {
        return new PlotValidationResult(true, null);
    }

    public static PlotValidationResult error(String message) {
        return new PlotValidationResult(false, message);
    }

    public boolean isValid() {
        return valid;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
