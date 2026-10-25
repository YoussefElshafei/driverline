package com.driverline.planning.variance.report;

import com.driverline.planning.account.AccountType;
import java.math.BigDecimal;

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
}
