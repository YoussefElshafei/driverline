package com.driverline.planning.variance.dto;

import com.driverline.planning.variance.VarianceDirection;
import com.driverline.planning.variance.VarianceLine;
import java.util.List;

/**
 * A 12-month trend for the whole company, one department, or one account.
 *
 * @param departmentCode    the department filter, or {@code null} for all departments
 * @param accountCode       the account filter, or {@code null} for net income / net contribution
 * @param scope             a readable description, e.g. "5300 Vehicle & Fuel - Fleet & Equipment"
 * @param direction         whether higher amounts are better for this scope
 * @param closedMonthsTotal budget vs. actual summed over the closed months only
 */
public record MonthlyVarianceReport(
        int year,
        String departmentCode,
        String accountCode,
        String scope,
        VarianceDirection direction,
        int latestActualMonth,
        List<MonthlyVarianceRow> months,
        VarianceLine closedMonthsTotal) {
}
