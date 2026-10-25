package com.driverline.planning.period;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * One month of a fiscal year. The fiscal year matches the calendar year, so month 1 is January.
 * Validating here means an impossible period (month 13) can't exist anywhere in the code.
 */
@Embeddable
public record FiscalPeriod(
        @Column(name = "fiscal_year", nullable = false) int year,
        @Column(name = "fiscal_month", nullable = false) int month) {

    public static final int MIN_YEAR = 2000;
    public static final int MAX_YEAR = 2100;

    public FiscalPeriod {
        if (year < MIN_YEAR || year > MAX_YEAR) {
            throw new IllegalArgumentException(
                    "Fiscal year must be between %d and %d, but was %d".formatted(MIN_YEAR, MAX_YEAR, year));
        }
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Fiscal month must be between 1 and 12, but was " + month);
        }
    }

    public static FiscalPeriod of(int year, int month) {
        return new FiscalPeriod(year, month);
    }
}
