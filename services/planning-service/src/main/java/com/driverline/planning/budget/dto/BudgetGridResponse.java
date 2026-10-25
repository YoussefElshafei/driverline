package com.driverline.planning.budget.dto;

import java.util.List;

/** @param departmentCode the department filter that was applied, or {@code null} for all departments */
public record BudgetGridResponse(int year, String departmentCode, List<BudgetGridRow> rows) {
}
