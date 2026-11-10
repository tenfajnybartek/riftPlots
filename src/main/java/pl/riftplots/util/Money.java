package pl.riftplots.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal MAX = new BigDecimal("999999999999.99");

    private Money() {
    }

    public static BigDecimal parse(String text) {
        BigDecimal amount = new BigDecimal(text);
        if (!isSafe(amount) || amount.stripTrailingZeros().scale() > 2) {
            throw new NumberFormatException("Kwota musi być nieujemna, mieć najwyżej 2 miejsca po przecinku i nie przekraczać " + MAX);
        }
        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    public static boolean isSafe(BigDecimal amount) {
        return amount != null && amount.signum() >= 0 && amount.compareTo(MAX) <= 0 && amount.scale() <= 8;
    }
}
