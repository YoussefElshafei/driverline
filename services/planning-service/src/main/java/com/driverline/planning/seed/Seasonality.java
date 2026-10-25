package com.driverline.planning.seed;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

/**
 * Month-by-month multipliers (January first) describing how a cost or revenue line moves through the year.
 * Each profile's factors add up to 12, so seasonality shifts money between months without changing the annual total.
 */
public enum Seasonality {
    FLAT("1.00", "1.00", "1.00", "1.00", "1.00", "1.00", "1.00", "1.00", "1.00", "1.00", "1.00", "1.00"),
    /** Recurring office contracts: steady, slightly softer in summer when tenants vacation. */
    CONTRACT("0.99", "0.99", "1.00", "1.00", "1.00", "0.99", "0.97", "0.98", "1.02", "1.02", "1.02", "1.02"),
    /** One-off deep cleans: spring cleaning (Apr-May) and back-to-school (Aug) peaks, quiet over winter holidays. */
    SPECIALTY("0.60", "0.65", "0.95", "1.40", "1.50", "1.10", "0.90", "1.35", "1.10", "0.95", "0.80", "0.70"),
    /** Hourly cleaning labour follows job volume. */
    LABOUR("0.97", "0.97", "1.00", "1.05", "1.06", "1.01", "0.98", "1.04", "1.01", "0.99", "0.96", "0.96"),
    /** Cleaning supplies follow job volume a little more strongly than labour. */
    SUPPLIES("0.95", "0.95", "1.00", "1.10", "1.12", "1.00", "0.95", "1.08", "1.00", "0.98", "0.94", "0.93"),
    /** Vans burn more fuel and need more upkeep in winter. */
    VEHICLE("1.10", "1.08", "1.00", "0.98", "1.00", "0.96", "0.94", "0.98", "0.98", "1.00", "0.98", "1.00"),
    /** Campaigns ahead of the spring and fall busy seasons. */
    MARKETING("0.80", "1.30", "1.40", "1.10", "0.90", "0.70", "0.60", "0.80", "1.30", "1.10", "0.90", "1.10"),
    /** Heating in winter, some cooling in summer. */
    UTILITIES("1.30", "1.25", "1.05", "0.90", "0.80", "0.90", "1.00", "1.00", "0.85", "0.90", "0.95", "1.10");

    private final List<BigDecimal> monthlyFactors;

    Seasonality(String... monthlyFactors) {
        if (monthlyFactors.length != 12) {
            throw new IllegalArgumentException("Seasonality needs exactly 12 monthly factors");
        }
        this.monthlyFactors = Arrays.stream(monthlyFactors).map(BigDecimal::new).toList();
    }

    /** @param month 1 (January) to 12 (December) */
    public BigDecimal factorFor(int month) {
        return monthlyFactors.get(month - 1);
    }

    public List<BigDecimal> monthlyFactors() {
        return monthlyFactors;
    }
}
