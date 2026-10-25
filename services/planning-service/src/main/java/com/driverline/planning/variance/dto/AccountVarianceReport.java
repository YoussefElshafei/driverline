package com.driverline.planning.variance.dto;

import java.util.List;

/** Profit-and-loss layout: revenue accounts, expense accounts, each combined across all departments. */
public record AccountVarianceReport(
        int year,
        int fromMonth,
        int toMonth,
        int latestActualMonth,
        VarianceSummary summary,
        List<AccountVarianceRow> revenue,
        List<AccountVarianceRow> expenses) {
}
