package com.driverline.planning.forecast.dto;

import java.util.List;

/** The full-year outlook of every scenario side by side, in scenario order (Base, Best, Worst). */
public record ScenarioComparison(int year, int latestActualMonth, List<ScenarioOutlook> scenarios) {

    public record ScenarioOutlook(String scenarioCode, String scenarioName, ForecastSummary summary) {
    }
}
