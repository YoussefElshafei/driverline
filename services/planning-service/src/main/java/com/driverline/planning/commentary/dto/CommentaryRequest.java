package com.driverline.planning.commentary.dto;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.variance.Favorability;
import com.driverline.planning.variance.VarianceLine;
import java.math.BigDecimal;
import java.util.List;

/**
 * The body sent to commentary-service's {@code POST /api/commentary}. The services don't share code
 * (ADR 0001), so this mirrors commentary-service's request; docs/api.md documents the contract.
 */
public record CommentaryRequest(
        String periodLabel,
        Figures revenue,
        Figures expenses,
        Figures netIncome,
        List<Highlight> highlights) {

    public record Figures(
            BigDecimal budget, BigDecimal actual, BigDecimal variance, BigDecimal variancePercent, Favorability favorability) {

        public static Figures from(VarianceLine line) {
            return new Figures(line.budget(), line.actual(), line.variance(), line.variancePercent(), line.favorability());
        }
    }

    public record Highlight(String accountName, String departmentName, AccountType accountType, Figures figures) {
    }
}
