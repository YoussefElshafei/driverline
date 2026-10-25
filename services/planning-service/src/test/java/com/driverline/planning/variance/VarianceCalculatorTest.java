package com.driverline.planning.variance;

import static com.driverline.planning.variance.Favorability.FAVORABLE;
import static com.driverline.planning.variance.Favorability.ON_BUDGET;
import static com.driverline.planning.variance.Favorability.UNFAVORABLE;
import static com.driverline.planning.variance.VarianceDirection.HIGHER_IS_BETTER;
import static com.driverline.planning.variance.VarianceDirection.LOWER_IS_BETTER;
import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.account.AccountType;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VarianceCalculatorTest {

    private static VarianceLine calculate(String budget, String actual, VarianceDirection direction) {
        return VarianceCalculator.calculate(new BigDecimal(budget), new BigDecimal(actual), direction);
    }

    @Nested
    class SignRules {

        @Test
        void revenueAboveBudgetIsFavorable() {
            VarianceLine line = calculate("1000.00", "1100.00", VarianceDirection.forAccountType(AccountType.REVENUE));

            assertThat(line.variance()).isEqualTo(new BigDecimal("100.00"));
            assertThat(line.variancePercent()).isEqualTo(new BigDecimal("10.0"));
            assertThat(line.favorability()).isEqualTo(FAVORABLE);
        }

        @Test
        void revenueBelowBudgetIsUnfavorable() {
            VarianceLine line = calculate("1000.00", "900.00", VarianceDirection.forAccountType(AccountType.REVENUE));

            assertThat(line.variance()).isEqualTo(new BigDecimal("-100.00"));
            assertThat(line.variancePercent()).isEqualTo(new BigDecimal("-10.0"));
            assertThat(line.favorability()).isEqualTo(UNFAVORABLE);
        }

        @Test
        void expenseAboveBudgetIsUnfavorable() {
            VarianceLine line = calculate("1000.00", "1100.00", VarianceDirection.forAccountType(AccountType.EXPENSE));

            assertThat(line.variance()).isEqualTo(new BigDecimal("100.00"));
            assertThat(line.variancePercent()).isEqualTo(new BigDecimal("10.0"));
            assertThat(line.favorability()).isEqualTo(UNFAVORABLE);
        }

        @Test
        void expenseBelowBudgetIsFavorable() {
            VarianceLine line = calculate("1000.00", "900.00", VarianceDirection.forAccountType(AccountType.EXPENSE));

            assertThat(line.variance()).isEqualTo(new BigDecimal("-100.00"));
            assertThat(line.favorability()).isEqualTo(FAVORABLE);
        }

        @Test
        void exactlyOnBudgetIsNeitherFavorableNorUnfavorable() {
            assertThat(calculate("500.00", "500.00", HIGHER_IS_BETTER).favorability()).isEqualTo(ON_BUDGET);
            assertThat(calculate("500.00", "500.00", LOWER_IS_BETTER).favorability()).isEqualTo(ON_BUDGET);
            assertThat(calculate("500.00", "500.00", LOWER_IS_BETTER).variancePercent()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void zeroBudgetHasNoPercentageButStillHasADirection() {
            VarianceLine unplannedExpense = calculate("0.00", "250.00", LOWER_IS_BETTER);
            VarianceLine unplannedRevenue = calculate("0.00", "250.00", HIGHER_IS_BETTER);

            assertThat(unplannedExpense.variancePercent()).isNull();
            assertThat(unplannedExpense.variance()).isEqualTo(new BigDecimal("250.00"));
            assertThat(unplannedExpense.favorability()).isEqualTo(UNFAVORABLE);
            assertThat(unplannedRevenue.favorability()).isEqualTo(FAVORABLE);
        }

        @Test
        void zeroBudgetAndZeroActualIsOnBudgetWithNoPercentage() {
            VarianceLine line = calculate("0.00", "0.00", LOWER_IS_BETTER);

            assertThat(line.favorability()).isEqualTo(ON_BUDGET);
            assertThat(line.variancePercent()).isNull();
        }

        @Test
        void actualOfZeroAgainstABudgetIsAFullHundredPercentVariance() {
            VarianceLine noSpend = calculate("400.00", "0.00", LOWER_IS_BETTER);

            assertThat(noSpend.variancePercent()).isEqualTo(new BigDecimal("-100.0"));
            assertThat(noSpend.favorability()).isEqualTo(FAVORABLE);
        }

        @Test
        void negativeBudgetUsesItsMagnitudeSoThePercentKeepsTheVarianceSign() {
            // A planned net loss of 200 that came in as a loss of 150: 50 better than plan.
            VarianceLine line = calculate("-200.00", "-150.00", HIGHER_IS_BETTER);

            assertThat(line.variance()).isEqualTo(new BigDecimal("50.00"));
            assertThat(line.variancePercent()).isEqualTo(new BigDecimal("25.0"));
            assertThat(line.favorability()).isEqualTo(FAVORABLE);
        }

        @Test
        void negativeActualOnARevenueAccountIsUnfavorable() {
            // Refunds exceeded sales for the month.
            VarianceLine line = calculate("100.00", "-20.00", HIGHER_IS_BETTER);

            assertThat(line.variance()).isEqualTo(new BigDecimal("-120.00"));
            assertThat(line.variancePercent()).isEqualTo(new BigDecimal("-120.0"));
            assertThat(line.favorability()).isEqualTo(UNFAVORABLE);
        }
    }

    @ParameterizedTest(name = "budget {0}, actual {1} -> {2}%")
    @CsvSource({
            "3.00, 4.00, 33.3",
            "8.00, 9.00, 12.5",
            "16.00, 17.00, 6.3",
            "295365.00, 279003.55, -5.5",
            "15435.00, 18703.24, 21.2"
    })
    void roundsThePercentageHalfUpToOneDecimal(String budget, String actual, String expectedPercent) {
        assertThat(calculate(budget, actual, LOWER_IS_BETTER).variancePercent()).isEqualTo(new BigDecimal(expectedPercent));
    }
}
