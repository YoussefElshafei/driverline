package com.driverline.planning.variance.dto;

import com.driverline.planning.variance.VarianceLine;
import java.util.List;

/**
 * A department's total and its accounts (the drill-down).
 *
 * @param netContribution the department's revenue minus its expenses; higher is better. For a cost centre with no
 *                        revenue this is negative, and spending less than budget shows as a favorable variance.
 */
public record DepartmentVarianceRow(
        String departmentCode, String departmentName, VarianceLine netContribution, List<AccountVarianceRow> accounts) {
}
