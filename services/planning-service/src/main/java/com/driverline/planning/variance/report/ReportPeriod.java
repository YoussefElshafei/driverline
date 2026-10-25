package com.driverline.planning.variance.report;

import com.driverline.planning.common.error.FieldErrorDetail;
import com.driverline.planning.common.error.RequestValidationException;
import java.util.List;

/** An inclusive range of months within one fiscal year, e.g. FY2026 January–September. */
public record ReportPeriod(int year, int fromMonth, int toMonth) {

    public ReportPeriod {
        if (fromMonth < 1 || toMonth > 12 || fromMonth > toMonth) {
            throw new RequestValidationException(
                    "The month range is invalid.",
                    List.of(new FieldErrorDetail(
                            "toMonth", "Months must satisfy 1 <= fromMonth <= toMonth <= 12, but got %d to %d"
                                    .formatted(fromMonth, toMonth))));
        }
    }

    /**
     * Fills in defaults: from January, to the latest month with actuals (or December if the year has none yet).
     * Comparing budget against months that haven't happened yet would make every expense look favorable.
     */
    public static ReportPeriod resolve(int year, Integer fromMonth, Integer toMonth, int latestActualMonth) {
        int defaultTo = latestActualMonth > 0 ? latestActualMonth : 12;
        return new ReportPeriod(year, fromMonth != null ? fromMonth : 1, toMonth != null ? toMonth : defaultTo);
    }

    public boolean includes(int month) {
        return month >= fromMonth && month <= toMonth;
    }
}
