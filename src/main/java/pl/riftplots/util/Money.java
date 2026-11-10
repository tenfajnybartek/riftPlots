package pl.riftplots.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    public static final BigDecimal MAX = new BigDecimal("999999999999.99");

    private Money() {
    }

    public static BigDecimal parse(String text) {
        BigDecimal amount = new BigDecimal(text);
        int decimals = pl.riftplots.config.Settings.integer("economy.decimal-places");
        if (!isSafe(amount) || amount.stripTrailingZeros().scale() > decimals) {
            throw new NumberFormatException(pl.riftplots.config.Texts.raw("validation.money", decimals, maximum()));
        }
        return amount.setScale(decimals, RoundingMode.UNNECESSARY);
    }

    public static boolean isSafe(BigDecimal amount) {
        return amount != null && amount.signum() >= 0 && amount.compareTo(maximum()) <= 0 && amount.scale() <= 8;
    }

    public static BigDecimal maximum() {
        return new BigDecimal(pl.riftplots.config.Settings.string("economy.maximum-transaction-amount")).min(MAX);
    }
}
