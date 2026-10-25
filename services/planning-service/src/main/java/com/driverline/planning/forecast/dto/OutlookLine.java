package com.driverline.planning.forecast.dto;

import com.driverline.planning.variance.Favorability;
import com.driverline.planning.variance.VarianceLine;
import java.math.BigDecimal;

/**
 * Full-year budget vs. full-year outlook (actuals so far + forecast for the rest of the year).
 *
 * @param variance outlook - budget, using the same sign and favorability rules as variance reports
 */
public record OutlookLine(
        BigDecimal budget,
        BigDecimal outlook,
        BigDecimal variance,
        BigDecimal variancePercent,
        Favorability favorability) {

    public static OutlookLine from(VarianceLine line) {
        return new OutlookLine(line.budget(), line.actual(), line.variance(), line.variancePercent(), line.favorability());
    }
}
