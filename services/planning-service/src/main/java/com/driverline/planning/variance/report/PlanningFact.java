package com.driverline.planning.variance.report;

import com.driverline.planning.account.AccountType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Budget and actual for one account, in one department, in one month of the report year.
 *
 * @param budget the budgeted amount, zero when no budget was entered
 * @param actual the actual amount, or {@code null} when no actual has been recorded for this cell
 */
public record PlanningFact(
        String accountCode,
        String accountName,
        AccountType accountType,
        String departmentCode,
        String departmentName,
        int month,
        BigDecimal budget,
        BigDecimal actual) {

    public boolean hasActual() {
        return actual != null;
    }

    /** Months with at least one actual recorded: their books are closed and they can be compared to budget. */
    public static Set<Integer> closedMonths(List<PlanningFact> facts) {
        return facts.stream().filter(PlanningFact::hasActual).map(PlanningFact::month).collect(Collectors.toSet());
    }
}
