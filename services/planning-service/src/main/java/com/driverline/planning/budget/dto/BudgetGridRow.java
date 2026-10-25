package com.driverline.planning.budget.dto;

import com.driverline.planning.account.AccountType;
import java.math.BigDecimal;
import java.util.List;

/**
 * One account in one department for a whole year.
 *
 * @param months 12 amounts, January first; {@code null} where no budget has been entered for that month
 * @param total  sum of the entered months
 */
public record BudgetGridRow(
        String accountCode,
        String accountName,
        AccountType accountType,
        String departmentCode,
        String departmentName,
        List<BigDecimal> months,
        BigDecimal total) {
}
