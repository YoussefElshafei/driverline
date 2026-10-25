package com.driverline.planning.variance.dto;

import java.util.List;

/** @param latestActualMonth the last month of the year with any actuals, or 0 if none yet */
public record DepartmentVarianceReport(
        int year,
        int fromMonth,
        int toMonth,
        int latestActualMonth,
        VarianceSummary summary,
        List<DepartmentVarianceRow> departments) {
}
