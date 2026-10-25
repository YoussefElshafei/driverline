package com.driverline.planning.forecast.dto;

import java.util.List;

/** @param netContribution the department's revenue minus expenses, full-year budget vs. outlook */
public record ForecastDepartment(
        String departmentCode, String departmentName, OutlookLine netContribution, List<ForecastLineRow> lines) {
}
