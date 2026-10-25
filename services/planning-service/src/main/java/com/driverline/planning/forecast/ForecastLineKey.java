package com.driverline.planning.forecast;

/** One forecastable line: an account in a department (for a given year). */
public record ForecastLineKey(String accountCode, String departmentCode) {
}
