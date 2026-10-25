package com.driverline.planning.forecast.engine;

import static com.driverline.planning.account.AccountType.EXPENSE;
import static com.driverline.planning.account.AccountType.REVENUE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.dto.MonthAmount;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ForecastEngineTest {

    private static final Set<Integer> JAN_TO_SEP = Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9);

    private final ForecastEngine engine = new ForecastEngine();

    private static BigDecimal money(String amount) {
        return new BigDecimal(amount);
    }

    /** A line with the same budget every month and the same actual in every closed month (Jan-Sep). */
    private static ForecastInputLine line(String account, AccountType type, String monthlyBudget, String monthlyActual,
            DriverSpec driver) {
        Map<Integer, BigDecimal> budgets = new HashMap<>();
        Map<Integer, BigDecimal> actuals = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            budgets.put(month, money(monthlyBudget));
            if (month <= 9 && monthlyActual != null) {
                actuals.put(month, money(monthlyActual));
            }
        }
        return new ForecastInputLine(account, "Account " + account, type, "DEPT", "Department", budgets, actuals, driver);
    }

    private static BigDecimal month(ForecastedLine line, int month) {
        return line.months().get(month - 1).amount();
    }

    @Test
    void unitsTimesRateForecastsOpenMonthsAndKeepsActualsForClosedMonths() {
        ForecastedLine contracts = engine.forecast(List.of(
                line("4000", REVENUE, "310590.00", "300000.00", DriverSpec.unitsTimesRate(money("62"), money("4700.00")))),
                JAN_TO_SEP).getFirst();

        assertThat(contracts.months().get(8)).isEqualTo(new MonthAmount(9, money("300000.00"), true));
        assertThat(contracts.months().get(9)).isEqualTo(new MonthAmount(10, money("291400.00"), false));
        assertThat(month(contracts, 12)).isEqualTo(money("291400.00"));
        // 9 x 300,000 actual + 3 x 291,400 forecast
        assertThat(contracts.outlookTotal()).isEqualTo(money("3574200.00"));
        assertThat(contracts.budgetTotal()).isEqualTo(money("3727080.00"));
    }

    @Test
    void percentOfRevenueFollowsTheMonthsTotalForecastRevenue() {
        List<ForecastedLine> lines = engine.forecast(List.of(
                line("4000", REVENUE, "0.00", "1.00", DriverSpec.unitsTimesRate(money("62"), money("4700.00"))),
                line("4100", REVENUE, "0.00", "1.00", DriverSpec.unitsTimesRate(money("16"), money("2900.00"))),
                line("5200", EXPENSE, "24696.00", "25000.00", DriverSpec.percentOfRevenue(money("7.6")))),
                JAN_TO_SEP);

        // 7.6% of (291,400 + 46,400) = 25,672.80
        assertThat(month(lines.get(2), 10)).isEqualTo(money("25672.80"));
        // Closed months still use the actual, not the percentage.
        assertThat(month(lines.get(2), 9)).isEqualTo(money("25000.00"));
    }

    @Test
    void percentOfRevenueWorksEvenWhenTheExpenseLineComesBeforeTheRevenueLines() {
        List<ForecastedLine> lines = engine.forecast(List.of(
                line("5200", EXPENSE, "0.00", null, DriverSpec.percentOfRevenue(money("10"))),
                line("4000", REVENUE, "0.00", null, DriverSpec.unitsTimesRate(money("10"), money("100.00")))),
                Set.of());

        assertThat(month(lines.getFirst(), 1)).isEqualTo(money("100.00"));
    }

    @Test
    void budgetMethodUsesEachMonthsBudget() {
        Map<Integer, BigDecimal> seasonal = Map.of(10, money("2457.00"), 11, money("2593.50"), 12, money("3003.00"));
        ForecastInputLine utilities = new ForecastInputLine("6400", "Utilities", EXPENSE, "ADMIN", "Administration",
                seasonal, Map.of(), DriverSpec.USE_BUDGET);

        ForecastedLine result = engine.forecast(List.of(utilities), JAN_TO_SEP).getFirst();

        assertThat(month(result, 10)).isEqualTo(money("2457.00"));
        assertThat(month(result, 11)).isEqualTo(money("2593.50"));
        assertThat(month(result, 12)).isEqualTo(money("3003.00"));
    }

    @Test
    void aClosedMonthWithNoActualForTheLineCountsAsZero() {
        ForecastedLine noActuals = engine.forecast(List.of(
                line("6100", EXPENSE, "500.00", null, DriverSpec.USE_BUDGET)), JAN_TO_SEP).getFirst();

        assertThat(noActuals.months().getFirst()).isEqualTo(new MonthAmount(1, money("0.00"), true));
        assertThat(month(noActuals, 10)).isEqualTo(money("500.00"));
    }

    @Test
    void forecastsAllTwelveMonthsWhenNothingIsClosedYet() {
        ForecastedLine nextYear = engine.forecast(List.of(
                line("5300", EXPENSE, "15000.00", null, DriverSpec.unitsTimesRate(money("14"), money("1250.00")))),
                Set.of()).getFirst();

        assertThat(nextYear.months()).noneMatch(MonthAmount::actual);
        assertThat(nextYear.outlookTotal()).isEqualTo(money("210000.00"));
    }

    @Test
    void roundsDriverResultsToCentsHalfUp() {
        ForecastedLine jobs = engine.forecast(List.of(
                line("4100", REVENUE, "0.00", null, DriverSpec.unitsTimesRate(money("12.5"), money("2899.99")))),
                Set.of()).getFirst();

        // 12.5 x 2,899.99 = 36,249.875 -> 36,249.88
        assertThat(month(jobs, 1)).isEqualTo(money("36249.88"));
    }

    @Test
    void rejectsARevenueLineThatIsAPercentageOfRevenue() {
        assertThatThrownBy(() -> engine.forecast(List.of(
                line("4000", REVENUE, "0.00", null, DriverSpec.percentOfRevenue(money("5")))), Set.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be a percentage of revenue");
    }

    @Test
    void scenariosWithDifferentDriversProduceDifferentOutlooks() {
        ForecastedLine worst = engine.forecast(List.of(
                line("4000", REVENUE, "0.00", "0.00", DriverSpec.unitsTimesRate(money("60"), money("4650.00")))),
                JAN_TO_SEP).getFirst();
        ForecastedLine best = engine.forecast(List.of(
                line("4000", REVENUE, "0.00", "0.00", DriverSpec.unitsTimesRate(money("64"), money("4725.00")))),
                JAN_TO_SEP).getFirst();

        assertThat(worst.outlookTotal()).isEqualTo(money("837000.00"));
        assertThat(best.outlookTotal()).isEqualTo(money("907200.00"));
    }
}
