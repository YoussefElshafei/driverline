package com.driverline.commentary;

import com.driverline.commentary.api.dto.AccountType;
import com.driverline.commentary.api.dto.CommentaryRequest;
import com.driverline.commentary.api.dto.Favorability;
import com.driverline.commentary.api.dto.VarianceFigures;
import com.driverline.commentary.api.dto.VarianceHighlight;
import java.math.BigDecimal;
import java.util.List;

/** Ace Janitorial Service (fictional), FY2026 January to September. */
public final class SampleRequests {

    private SampleRequests() {
    }

    public static VarianceFigures figures(String budget, String actual, String variance, String percent,
            Favorability favorability) {
        return new VarianceFigures(new BigDecimal(budget), new BigDecimal(actual), new BigDecimal(variance),
                percent == null ? null : new BigDecimal(percent), favorability);
    }

    public static CommentaryRequest aceJanitorialFy2026() {
        return new CommentaryRequest(
                "FY2026, January to September",
                figures("3223605.00", "3169643.42", "-53961.58", "-1.7", Favorability.UNFAVORABLE),
                figures("2936125.50", "2960379.17", "24253.67", "0.8", Favorability.UNFAVORABLE),
                figures("287479.50", "209264.25", "-78215.25", "-27.2", Favorability.UNFAVORABLE),
                List.of(
                        new VarianceHighlight("Contract Cleaning Revenue", "Sales & Account Management", AccountType.REVENUE,
                                figures("2795145.00", "2751085.96", "-44059.04", "-1.6", Favorability.UNFAVORABLE)),
                        new VarianceHighlight("Cleaning Supplies", "Operations", AccountType.EXPENSE,
                                figures("229446.00", "246216.23", "16770.23", "7.3", Favorability.UNFAVORABLE)),
                        new VarianceHighlight("Vehicle & Fuel", "Fleet & Equipment", AccountType.EXPENSE,
                                figures("142065.00", "156518.61", "14453.61", "10.2", Favorability.UNFAVORABLE)),
                        new VarianceHighlight("Wages - Cleaning Staff", "Operations", AccountType.EXPENSE,
                                figures("1431342.00", "1423830.76", "-7511.24", "-0.5", Favorability.FAVORABLE))));
    }
}
