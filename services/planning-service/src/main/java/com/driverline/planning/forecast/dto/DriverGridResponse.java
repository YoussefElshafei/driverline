package com.driverline.planning.forecast.dto;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.ForecastMethod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Every forecastable line for a year with its method and driver values per scenario. */
public record DriverGridResponse(
        int year, int latestActualMonth, List<ScenarioResponse> scenarios, List<DriverLine> lines) {

    /**
     * @param configured           false for lines that have no forecast configuration yet (they use their budget)
     * @param values               driver values keyed by scenario code; empty for budget lines
     * @param averageOpenMonthBudget the line's average monthly budget over the forecast months, for comparison
     *                             with the driver result; {@code null} when no months are left to forecast
     */
    public record DriverLine(
            String accountCode,
            String accountName,
            AccountType accountType,
            String departmentCode,
            String departmentName,
            ForecastMethod method,
            String quantityLabel,
            String rateLabel,
            boolean configured,
            Map<String, DriverValues> values,
            BigDecimal averageOpenMonthBudget) {
    }

    public record DriverValues(BigDecimal quantity, BigDecimal rate, BigDecimal percent) {
    }
}
