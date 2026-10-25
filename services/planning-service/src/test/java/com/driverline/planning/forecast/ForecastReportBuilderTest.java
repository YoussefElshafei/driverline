package com.driverline.planning.forecast;

import static com.driverline.planning.account.AccountType.EXPENSE;
import static com.driverline.planning.account.AccountType.REVENUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.dto.ForecastReport;
import com.driverline.planning.forecast.dto.ForecastSummary;
import com.driverline.planning.forecast.dto.OutlookLine;
import com.driverline.planning.forecast.engine.ForecastedLine;
import com.driverline.planning.variance.Favorability;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForecastReportBuilderTest {

    private final ForecastReportBuilder builder = new ForecastReportBuilder();

    private static ForecastedLine line(String account, AccountType type, String department, String budget, String outlook) {
        return new ForecastedLine(account, "Account " + account, type, department, "Dept " + department,
                ForecastMethod.UNITS_X_RATE, List.of(), new BigDecimal(budget), new BigDecimal(outlook));
    }

    private static final List<ForecastedLine> LINES = List.of(
            line("4000", REVENUE, "SALES", "1000.00", "900.00"),
            line("6100", EXPENSE, "SALES", "100.00", "80.00"),
            line("5300", EXPENSE, "FLEET", "200.00", "260.00"));

    private static void assertOutlook(OutlookLine line, String budget, String outlook, String variance, String percent,
            Favorability favorability) {
        assertThat(line.budget()).isEqualTo(new BigDecimal(budget));
        assertThat(line.outlook()).isEqualTo(new BigDecimal(outlook));
        assertThat(line.variance()).isEqualTo(new BigDecimal(variance));
        assertThat(line.variancePercent()).isEqualTo(new BigDecimal(percent));
        assertThat(line.favorability()).isEqualTo(favorability);
    }

    @Test
    void summarisesFullYearOutlookAgainstBudgetWithTheVarianceRules() {
        ForecastSummary summary = builder.summarize(LINES);

        assertOutlook(summary.revenue(), "1000.00", "900.00", "-100.00", "-10.0", Favorability.UNFAVORABLE);
        assertOutlook(summary.expenses(), "300.00", "340.00", "40.00", "13.3", Favorability.UNFAVORABLE);
        // (1000 - 300) budget vs (900 - 340) outlook
        assertOutlook(summary.netIncome(), "700.00", "560.00", "-140.00", "-20.0", Favorability.UNFAVORABLE);
    }

    @Test
    void groupsLinesByDepartmentWithNetContribution() {
        Scenario base = mock(Scenario.class);
        when(base.getCode()).thenReturn("BASE");
        when(base.getName()).thenReturn("Base");

        ForecastReport report = builder.build(2026, base, 9, LINES);

        assertThat(report.scenarioName()).isEqualTo("Base");
        assertThat(report.departments()).extracting(d -> d.departmentCode()).containsExactly("FLEET", "SALES");
        assertOutlook(report.departments().get(1).netContribution(), "900.00", "820.00", "-80.00", "-8.9",
                Favorability.UNFAVORABLE);
        // An expense line under budget is favorable.
        assertThat(report.departments().get(1).lines().get(1).outlook().favorability()).isEqualTo(Favorability.FAVORABLE);
    }
}
