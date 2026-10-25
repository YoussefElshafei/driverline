package com.driverline.planning.forecast.dto;

import java.util.List;

/** @param latestActualMonth months up to here are actuals; later months are forecast (0 = all forecast) */
public record ForecastReport(
        int year,
        String scenarioCode,
        String scenarioName,
        int latestActualMonth,
        ForecastSummary summary,
        List<ForecastDepartment> departments) {
}
