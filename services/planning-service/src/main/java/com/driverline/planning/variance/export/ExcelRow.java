package com.driverline.planning.variance.export;

import com.driverline.planning.variance.Favorability;
import com.driverline.planning.variance.VarianceLine;
import java.math.BigDecimal;

/**
 * One spreadsheet row.
 *
 * @param outlineLevel 0 for top-level rows, 1 for detail rows that Excel can collapse under the row above
 * @param actual       {@code null} for a month that isn't closed yet (variance cells are then left empty)
 */
public record ExcelRow(
        String label, Kind kind, int outlineLevel, BigDecimal budget, BigDecimal actual, Favorability favorability) {

    public enum Kind {
        /** A heading with no numbers, e.g. "Revenue". */
        SECTION,
        DETAIL,
        SUBTOTAL,
        TOTAL
    }

    public static ExcelRow section(String label) {
        return new ExcelRow(label, Kind.SECTION, 0, null, null, null);
    }

    public static ExcelRow of(String label, Kind kind, int outlineLevel, VarianceLine line) {
        return new ExcelRow(label, kind, outlineLevel, line.budget(), line.actual(), line.favorability());
    }

    public static ExcelRow notClosed(String label, BigDecimal budget) {
        return new ExcelRow(label, Kind.DETAIL, 0, budget, null, null);
    }

    public boolean hasNumbers() {
        return kind != Kind.SECTION;
    }
}
