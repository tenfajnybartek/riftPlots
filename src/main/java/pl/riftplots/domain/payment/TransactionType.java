package pl.riftplots.domain.payment;

public enum TransactionType {
    PURCHASE,
    TAX,
    ADVANCE_TAX,
    REFUND,
    ADMIN_ADJUSTMENT;

    public static TransactionType parse(String input) {
        if (input == null || input.isBlank()) {
            return TAX;
        }
        try {
            return TransactionType.valueOf(input.toUpperCase());
        } catch (IllegalArgumentException e) {
            return TAX;
        }
    }
}
