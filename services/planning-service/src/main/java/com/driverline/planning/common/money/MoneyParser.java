package com.driverline.planning.common.money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Pattern;

/**
 * Parses money typed by people or exported from spreadsheets into an exact {@link BigDecimal} with 2 decimals.
 * Accepts {@code 1234.5}, {@code 1,234.50}, {@code $1,234.50}, {@code -1234.50} and accounting-style
 * negatives {@code (1,234.50)}. Rejects anything with fractions of a cent rather than silently rounding.
 */
public final class MoneyParser {

    /** Up to 9,999,999,999,999.99, which matches the database column NUMERIC(15,2). */
    public static final int MAX_WHOLE_DIGITS = 13;
    public static final int SCALE = 2;

    private static final Pattern PLAIN_NUMBER = Pattern.compile("-?\\d+(\\.\\d+)?");

    private MoneyParser() {
    }

    /**
     * @throws InvalidMoneyException with a user-facing message if the text isn't a valid amount
     */
    public static BigDecimal parse(String text) {
        String cleaned = text.trim();
        boolean accountingNegative = cleaned.startsWith("(") && cleaned.endsWith(")");
        if (accountingNegative) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        cleaned = cleaned.replace("$", "").replace(",", "").trim();

        if (!PLAIN_NUMBER.matcher(cleaned).matches()) {
            throw new InvalidMoneyException("is not a valid amount");
        }
        BigDecimal amount = new BigDecimal(cleaned);
        if (accountingNegative) {
            if (amount.signum() < 0) {
                throw new InvalidMoneyException("is not a valid amount");
            }
            amount = amount.negate();
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidMoneyException("has more than 2 decimal places");
        }
        if (amount.precision() - amount.scale() > MAX_WHOLE_DIGITS) {
            throw new InvalidMoneyException("is too large");
        }
        // Safe: the scale check above guarantees no digits are lost.
        return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static class InvalidMoneyException extends IllegalArgumentException {

        public InvalidMoneyException(String message) {
            super(message);
        }
    }
}
