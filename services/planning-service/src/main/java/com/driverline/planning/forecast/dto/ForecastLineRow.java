package com.driverline.planning.forecast.dto;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.ForecastMethod;
import java.util.List;

/** @param months 12 amounts, each marked as an actual or a forecast */
public record ForecastLineRow(
        String accountCode,
        String accountName,
        AccountType accountType,
        ForecastMethod method,
        List<MonthAmount> months,
        OutlookLine outlook) {
}
