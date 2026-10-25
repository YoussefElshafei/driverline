package com.driverline.planning.variance.dto;

import com.driverline.planning.variance.VarianceLine;

/** Company totals: revenue and expenses as entered, and net income = revenue - expenses. */
public record VarianceSummary(VarianceLine revenue, VarianceLine expenses, VarianceLine netIncome) {
}
