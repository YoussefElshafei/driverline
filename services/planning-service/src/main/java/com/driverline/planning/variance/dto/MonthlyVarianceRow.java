package com.driverline.planning.variance.dto;

import com.driverline.planning.variance.VarianceLine;
import java.math.BigDecimal;

/**
 * @param closed   true when the month has actuals recorded (for any account), so it can be compared to budget
 * @param variance budget vs. actual for a closed month; {@code null} for a month that hasn't been closed yet
 */
public record MonthlyVarianceRow(int month, BigDecimal budget, boolean closed, VarianceLine variance) {
}
