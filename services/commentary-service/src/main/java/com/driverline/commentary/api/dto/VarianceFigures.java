package com.driverline.commentary.api.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * A budget vs. actual line, as calculated by planning-service. This service never recalculates variances;
 * it only describes them. Amounts may arrive as JSON strings ("1234.50") or numbers.
 *
 * @param variancePercent {@code null} when the budget is zero
 */
public record VarianceFigures(
        @NotNull BigDecimal budget,
        @NotNull BigDecimal actual,
        @NotNull BigDecimal variance,
        BigDecimal variancePercent,
        @NotNull Favorability favorability) {
}
