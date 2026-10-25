package com.driverline.planning.variance;

import java.math.BigDecimal;

/**
 * Budget vs. actual for one line of a report.
 *
 * @param variance        actual - budget (positive means actual came in higher than planned)
 * @param variancePercent variance as a percentage of |budget|, 1 decimal place; {@code null} when the budget is zero
 *                        (a percentage of nothing is undefined, so the UI shows "n/a")
 */
public record VarianceLine(
        BigDecimal budget,
        BigDecimal actual,
        BigDecimal variance,
        BigDecimal variancePercent,
        Favorability favorability) {
}
