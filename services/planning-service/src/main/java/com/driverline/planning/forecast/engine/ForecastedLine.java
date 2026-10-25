package com.driverline.planning.forecast.engine;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.ForecastMethod;
import com.driverline.planning.forecast.dto.MonthAmount;
import java.math.BigDecimal;
import java.util.List;

/**
 * One line's full-year outlook.
 *
 * @param months       12 amounts: actuals for closed months, forecast for the rest
 * @param budgetTotal  the full-year budget
 * @param outlookTotal actuals + forecast for the full year
 */
public record ForecastedLine(
        String accountCode,
        String accountName,
        AccountType accountType,
        String departmentCode,
        String departmentName,
        ForecastMethod method,
        List<MonthAmount> months,
        BigDecimal budgetTotal,
        BigDecimal outlookTotal) {
}
