package com.driverline.planning.forecast.engine;

import com.driverline.planning.account.AccountType;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Everything the engine needs for one account in one department.
 *
 * @param budgetByMonth budget amounts keyed by month (missing months have no budget)
 * @param actualByMonth actual amounts keyed by month (missing months have no actual)
 */
public record ForecastInputLine(
        String accountCode,
        String accountName,
        AccountType accountType,
        String departmentCode,
        String departmentName,
        Map<Integer, BigDecimal> budgetByMonth,
        Map<Integer, BigDecimal> actualByMonth,
        DriverSpec driver) {

    public ForecastInputLine {
        budgetByMonth = Map.copyOf(budgetByMonth);
        actualByMonth = Map.copyOf(actualByMonth);
    }
}
