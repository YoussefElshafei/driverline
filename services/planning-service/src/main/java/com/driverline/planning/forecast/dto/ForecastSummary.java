package com.driverline.planning.forecast.dto;

public record ForecastSummary(OutlookLine revenue, OutlookLine expenses, OutlookLine netIncome) {
}
