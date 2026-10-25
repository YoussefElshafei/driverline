package com.driverline.commentary.generation;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Formats amounts for commentary: whole dollars with thousands separators ("$44,059") and one-decimal percents.
 * Commentary rounds to whole dollars because leadership reads it; the reports keep the exact cents.
 */
public final class Amounts {

    private Amounts() {
    }

    /** "$44,059" for 44059.04 or -44059.04 (the direction is described in words). */
    public static String dollars(BigDecimal amount) {
        return "$" + wholeNumber(amount.abs());
    }

    /** "-$44,059" or "$16,770", keeping the sign, for the data given to the language model. */
    public static String signedDollars(BigDecimal amount) {
        return (amount.signum() < 0 ? "-" : "") + dollars(amount);
    }

    /** "1.6%" from 1.6 or -1.6; "n/a" when there is no percentage (zero budget). */
    public static String percent(BigDecimal percent) {
        return percent == null ? "n/a" : percent.abs().setScale(1, RoundingMode.HALF_UP).toPlainString() + "%";
    }

    /** The amount rounded half-up to whole dollars, as a plain number (used to verify model output). */
    public static BigDecimal roundedToDollars(BigDecimal amount) {
        return amount.abs().setScale(0, RoundingMode.HALF_UP);
    }

    private static String wholeNumber(BigDecimal amount) {
        // Round with BigDecimal first (exact, half-up), then let DecimalFormat only add the thousands separators.
        // DecimalFormat isn't thread-safe, so create one per call; commentary formats only a few dozen numbers.
        DecimalFormat format = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.US));
        return format.format(amount.setScale(0, RoundingMode.HALF_UP));
    }
}
