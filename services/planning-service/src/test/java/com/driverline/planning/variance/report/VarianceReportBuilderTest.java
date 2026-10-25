package com.driverline.planning.variance.report;

import static com.driverline.planning.account.AccountType.EXPENSE;
import static com.driverline.planning.account.AccountType.REVENUE;
import static com.driverline.planning.variance.Favorability.FAVORABLE;
import static com.driverline.planning.variance.Favorability.ON_BUDGET;
import static com.driverline.planning.variance.Favorability.UNFAVORABLE;
import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.variance.Favorability;
import com.driverline.planning.variance.VarianceDirection;
import com.driverline.planning.variance.VarianceLine;
import com.driverline.planning.variance.dto.AccountVarianceReport;
import com.driverline.planning.variance.dto.AccountVarianceRow;
import com.driverline.planning.variance.dto.DepartmentVarianceReport;
import com.driverline.planning.variance.dto.DepartmentVarianceRow;
import com.driverline.planning.variance.report.VarianceReportBuilder.MonthlyTrend;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Hand-checkable data, three months with actuals for January and February only:
 *
 * <pre>
 * SALES 4000 revenue    Jan 1000 / 1100   Feb 1000 / 900    Mar 1000 / -
 * SALES 6100 marketing  Jan  100 /  120   Feb  100 / (none) Mar  100 / -
 * OPS   5200 supplies   Jan  500 /  450   Feb  500 / 500    Mar  500 / -
 * OPS   5300 fuel       Feb no budget / 75
 * </pre>
 */
class VarianceReportBuilderTest {

    private final VarianceReportBuilder builder = new VarianceReportBuilder();

    private static PlanningFact fact(String account, AccountType type, String department, int month,
            String budget, String actual) {
        return new PlanningFact(account, "Account " + account, type, department, "Dept " + department, month,
                new BigDecimal(budget), actual == null ? null : new BigDecimal(actual));
    }

    private static final List<PlanningFact> FACTS = List.of(
            fact("4000", REVENUE, "SALES", 1, "1000.00", "1100.00"),
            fact("4000", REVENUE, "SALES", 2, "1000.00", "900.00"),
            fact("4000", REVENUE, "SALES", 3, "1000.00", null),
            fact("6100", EXPENSE, "SALES", 1, "100.00", "120.00"),
            fact("6100", EXPENSE, "SALES", 2, "100.00", null),
            fact("6100", EXPENSE, "SALES", 3, "100.00", null),
            fact("5200", EXPENSE, "OPS", 1, "500.00", "450.00"),
            fact("5200", EXPENSE, "OPS", 2, "500.00", "500.00"),
            fact("5200", EXPENSE, "OPS", 3, "500.00", null),
            fact("5300", EXPENSE, "OPS", 2, "0.00", "75.00"));

    private static final ReportPeriod JAN_TO_FEB = new ReportPeriod(2026, 1, 2);

    private static void assertLine(VarianceLine line, String budget, String actual, String variance,
            String percent, Favorability favorability) {
        assertThat(line.budget()).isEqualTo(new BigDecimal(budget));
        assertThat(line.actual()).isEqualTo(new BigDecimal(actual));
        assertThat(line.variance()).isEqualTo(new BigDecimal(variance));
        assertThat(line.variancePercent()).isEqualTo(percent == null ? null : new BigDecimal(percent));
        assertThat(line.favorability()).isEqualTo(favorability);
    }

    @Nested
    class ByDepartment {

        private final DepartmentVarianceReport report = builder.byDepartment(FACTS, JAN_TO_FEB);

        @Test
        void netsRevenueAndExpensesIntoEachDepartmentsContribution() {
            DepartmentVarianceRow sales = report.departments().get(1);
            DepartmentVarianceRow ops = report.departments().getFirst();

            // SALES: (2000 - 200) budget vs (2000 - 120) actual
            assertLine(sales.netContribution(), "1800.00", "1880.00", "80.00", "4.4", FAVORABLE);
            // OPS: -(1000 + 0) budget vs -(950 + 75) actual: spent 25 more than planned
            assertLine(ops.netContribution(), "-1000.00", "-1025.00", "-25.00", "-2.5", UNFAVORABLE);
        }

        @Test
        void listsDepartmentsAndTheirAccountsInCodeOrder() {
            assertThat(report.departments()).extracting(DepartmentVarianceRow::departmentCode)
                    .containsExactly("OPS", "SALES");
            assertThat(report.departments().getFirst().accounts()).extracting(AccountVarianceRow::accountCode)
                    .containsExactly("5200", "5300");
        }

        @Test
        void drillDownAccountsAddUpToTheDepartmentTotal() {
            for (DepartmentVarianceRow department : report.departments()) {
                BigDecimal accountsBudget = department.accounts().stream()
                        .map(a -> a.accountType().profitImpact(a.line().budget()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal accountsActual = department.accounts().stream()
                        .map(a -> a.accountType().profitImpact(a.line().actual()))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                assertThat(accountsBudget).isEqualByComparingTo(department.netContribution().budget());
                assertThat(accountsActual).isEqualByComparingTo(department.netContribution().actual());
            }
        }

        @Test
        void accountRowsKeepNaturalSignsAndTypeAwareFlags() {
            List<AccountVarianceRow> sales = report.departments().get(1).accounts();

            assertLine(sales.get(0).line(), "2000.00", "2000.00", "0.00", "0.0", ON_BUDGET);
            // February marketing has no actual in a closed month, so it counts as zero spend.
            assertLine(sales.get(1).line(), "200.00", "120.00", "-80.00", "-40.0", FAVORABLE);
        }

        @Test
        void anUnbudgetedExpenseHasNoPercentageAndIsUnfavorable() {
            AccountVarianceRow fuel = report.departments().getFirst().accounts().get(1);

            assertLine(fuel.line(), "0.00", "75.00", "75.00", null, UNFAVORABLE);
        }

        @Test
        void summarisesRevenueExpensesAndNetIncome() {
            assertLine(report.summary().revenue(), "2000.00", "2000.00", "0.00", "0.0", ON_BUDGET);
            assertLine(report.summary().expenses(), "1200.00", "1145.00", "-55.00", "-4.6", FAVORABLE);
            assertLine(report.summary().netIncome(), "800.00", "855.00", "55.00", "6.9", FAVORABLE);
        }

        @Test
        void departmentContributionsAddUpToNetIncome() {
            BigDecimal total = report.departments().stream()
                    .map(d -> d.netContribution().actual())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(total).isEqualByComparingTo(report.summary().netIncome().actual());
        }

        @Test
        void reportsThePeriodAndTheLatestMonthWithActuals() {
            assertThat(report.fromMonth()).isEqualTo(1);
            assertThat(report.toMonth()).isEqualTo(2);
            assertThat(report.latestActualMonth()).isEqualTo(2);
        }

        @Test
        void onlyCountsMonthsInsideTheSelectedRange() {
            DepartmentVarianceReport february = builder.byDepartment(FACTS, new ReportPeriod(2026, 2, 2));

            assertLine(february.summary().revenue(), "1000.00", "900.00", "-100.00", "-10.0", UNFAVORABLE);
        }
    }

    @Nested
    class ByAccount {

        @Test
        void combinesTheSameAccountAcrossDepartmentsAndSplitsRevenueFromExpenses() {
            List<PlanningFact> salaries = List.of(
                    fact("4000", REVENUE, "SALES", 1, "1000.00", "1000.00"),
                    fact("5100", EXPENSE, "SALES", 1, "300.00", "310.00"),
                    fact("5100", EXPENSE, "OPS", 1, "700.00", "690.00"));

            AccountVarianceReport report = builder.byAccount(salaries, new ReportPeriod(2026, 1, 1));

            assertThat(report.revenue()).extracting(AccountVarianceRow::accountCode).containsExactly("4000");
            assertThat(report.expenses()).singleElement().satisfies(row ->
                    assertLine(row.line(), "1000.00", "1000.00", "0.00", "0.0", ON_BUDGET));
        }
    }

    @Nested
    class ByMonth {

        @Test
        void netIncomeTrendLeavesMonthsWithoutActualsOpen() {
            MonthlyTrend trend = builder.byMonth(FACTS, null, null, VarianceDirection.HIGHER_IS_BETTER);

            assertThat(trend.months()).hasSize(12);
            // January net: budget 1000 - 100 - 500 = 400, actual 1100 - 120 - 450 = 530
            assertLine(trend.months().get(0).variance(), "400.00", "530.00", "130.00", "32.5", FAVORABLE);
            assertThat(trend.months().get(2).closed()).isFalse();
            assertThat(trend.months().get(2).variance()).isNull();
            assertThat(trend.months().get(2).budget()).isEqualTo(new BigDecimal("400.00"));
            assertThat(trend.months().get(11).budget()).isEqualTo(new BigDecimal("0.00"));
        }

        @Test
        void closedMonthsTotalExcludesMonthsWithoutActuals() {
            MonthlyTrend trend = builder.byMonth(FACTS, null, null, VarianceDirection.HIGHER_IS_BETTER);

            assertLine(trend.closedMonthsTotal(), "800.00", "855.00", "55.00", "6.9", FAVORABLE);
            assertThat(trend.latestActualMonth()).isEqualTo(2);
        }

        @Test
        void singleAccountTrendUsesNaturalSignsAndCountsAMissingActualAsZero() {
            MonthlyTrend marketing = builder.byMonth(FACTS, "SALES", "6100", VarianceDirection.LOWER_IS_BETTER);

            assertLine(marketing.months().get(1).variance(), "100.00", "0.00", "-100.00", "-100.0", FAVORABLE);
        }

        @Test
        void departmentTrendOnlyIncludesThatDepartment() {
            MonthlyTrend ops = builder.byMonth(FACTS, "OPS", null, VarianceDirection.HIGHER_IS_BETTER);

            assertLine(ops.months().get(1).variance(), "-500.00", "-575.00", "-75.00", "-15.0", UNFAVORABLE);
        }
    }

    @Test
    void aYearWithNoDataProducesAnEmptyReportWithZeroTotals() {
        DepartmentVarianceReport empty = builder.byDepartment(List.of(), new ReportPeriod(2030, 1, 12));

        assertThat(empty.departments()).isEmpty();
        assertThat(empty.latestActualMonth()).isZero();
        assertLine(empty.summary().netIncome(), "0.00", "0.00", "0.00", null, ON_BUDGET);
    }
}
