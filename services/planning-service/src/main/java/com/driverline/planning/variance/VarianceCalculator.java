package com.driverline.planning.variance;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * The variance rules, in one place:
 * <ul>
 *   <li>variance = actual - budget</li>
 *   <li>variance % = variance / |budget| x 100, rounded half-up to 1 decimal; undefined (null) when budget is 0</li>
 *   <li>favorable when the variance helps profit: above budget for revenue, below budget for expenses</li>
 * </ul>
 * Dividing by |budget| keeps the percentage's sign equal to the variance's sign even for negative budgets (credits).
 */
public final class VarianceCalculator {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final int PERCENT_SCALE = 1;

    private VarianceCalculator() {
    }

    public static VarianceLine calculate(BigDecimal budget, BigDecimal actual, VarianceDirection direction) {
        BigDecimal variance = actual.subtract(budget);
        return new VarianceLine(budget, actual, variance, percentOf(variance, budget), favorability(variance, direction));
    }

    private static BigDecimal percentOf(BigDecimal variance, BigDecimal budget) {
        if (budget.signum() == 0) {
            return null;
        }
        // Multiply before dividing so there is exactly one rounding step.
        return variance.multiply(ONE_HUNDRED).divide(budget.abs(), PERCENT_SCALE, RoundingMode.HALF_UP);
    }

    private static Favorability favorability(BigDecimal variance, VarianceDirection direction) {
        if (variance.signum() == 0) {
            return Favorability.ON_BUDGET;
        }
        boolean actualAboveBudget = variance.signum() > 0;
        boolean favorable = direction == VarianceDirection.HIGHER_IS_BETTER ? actualAboveBudget : !actualAboveBudget;
        return favorable ? Favorability.FAVORABLE : Favorability.UNFAVORABLE;
    }
}
