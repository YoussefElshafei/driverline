package com.driverline.planning.seed;

import static com.driverline.planning.account.AccountType.EXPENSE;
import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.period.FiscalPeriod;
import com.driverline.planning.seed.DemoCompanyProfile.AccountSeed;
import com.driverline.planning.seed.DemoCompanyProfile.DepartmentSeed;
import com.driverline.planning.seed.DemoCompanyProfile.LinePlan;
import com.driverline.planning.seed.DemoCompanyProfile.PlanYear;
import com.driverline.planning.seed.DemoCompanyProfile.StoryEvent;
import com.driverline.planning.seed.DemoDataset.SeedAmount;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SeedDataGeneratorTest {

    private final SeedDataGenerator generator = new SeedDataGenerator();

    /** One fuel line in one department with SPECIALTY seasonality: easy to calculate by hand. */
    private static DemoCompanyProfile singleLineProfile(int noiseBasisPoints, List<StoryEvent> events) {
        return singleLineProfile(noiseBasisPoints, events, List.of(new PlanYear(2026, new BigDecimal("1.05"), 6)), 42L);
    }

    private static DemoCompanyProfile singleLineProfile(
            int noiseBasisPoints, List<StoryEvent> events, List<PlanYear> years, long seed) {
        return new DemoCompanyProfile(
                "Test Co",
                List.of(new DepartmentSeed("OPS", "Operations")),
                List.of(new AccountSeed("5300", "Vehicle & Fuel", EXPENSE)),
                List.of(new LinePlan("5300", "OPS", new BigDecimal("1000.00"), Seasonality.SPECIALTY, noiseBasisPoints)),
                years,
                events,
                seed);
    }

    private static BigDecimal amountFor(List<SeedAmount> amounts, int year, int month) {
        return amounts.stream()
                .filter(a -> a.period().equals(FiscalPeriod.of(year, month)))
                .findFirst()
                .orElseThrow()
                .amount();
    }

    @Nested
    class Budgets {

        @Test
        void budgetIsMonthlyBaseTimesSeasonalityTimesGrowth() {
            DemoDataset dataset = generator.generate(singleLineProfile(0, List.of()));

            // 1000.00 x 1.50 (May) x 1.05 = 1575.00
            assertThat(amountFor(dataset.budgets(), 2026, 5)).isEqualTo(new BigDecimal("1575.00"));
            // 1000.00 x 0.60 (January) x 1.05 = 630.00
            assertThat(amountFor(dataset.budgets(), 2026, 1)).isEqualTo(new BigDecimal("630.00"));
        }

        @Test
        void everyYearGetsAFullTwelveMonthBudget() {
            DemoDataset dataset = generator.generate(singleLineProfile(0, List.of()));

            assertThat(dataset.budgets()).extracting(b -> b.period().month())
                    .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        }
    }

    @Nested
    class Actuals {

        @Test
        void actualsStopAtTheLastActualMonth() {
            DemoDataset dataset = generator.generate(singleLineProfile(0, List.of()));

            assertThat(dataset.actuals()).extracting(a -> a.period().month()).containsExactly(1, 2, 3, 4, 5, 6);
        }

        @Test
        void yearWithNoActualsYetProducesOnlyBudget() {
            var futureYear = List.of(new PlanYear(2027, BigDecimal.ONE, 0));
            DemoDataset dataset = generator.generate(singleLineProfile(0, List.of(), futureYear, 42L));

            assertThat(dataset.budgets()).hasSize(12);
            assertThat(dataset.actuals()).isEmpty();
        }

        @Test
        void actualEqualsBudgetWhenThereIsNoNoiseAndNoEvent() {
            DemoDataset dataset = generator.generate(singleLineProfile(0, List.of()));

            assertThat(dataset.actuals()).allSatisfy(actual ->
                    assertThat(actual.amount()).isEqualTo(amountFor(dataset.budgets(), 2026, actual.period().month())));
        }

        @Test
        void storyEventChangesActualsOnlyInsideItsMonths() {
            var fuelSpike = new StoryEvent("5300", 2026, 4, 5, new BigDecimal("1.18"), "Fuel spike");
            DemoDataset dataset = generator.generate(singleLineProfile(0, List.of(fuelSpike)));

            // April budget 1000.00 x 1.40 x 1.05 = 1470.00; actual 1470.00 x 1.18 = 1734.60
            assertThat(amountFor(dataset.actuals(), 2026, 4)).isEqualTo(new BigDecimal("1734.60"));
            // March is outside the event: actual = budget = 1000.00 x 0.95 x 1.05 = 997.50
            assertThat(amountFor(dataset.actuals(), 2026, 3)).isEqualTo(new BigDecimal("997.50"));
        }

        @Test
        void overlappingStoryEventsMultiply() {
            var up = new StoryEvent("5300", 2026, 4, 4, new BigDecimal("1.10"), "Up");
            var down = new StoryEvent("5300", 2026, 4, 4, new BigDecimal("0.90"), "Down");
            DemoDataset dataset = generator.generate(singleLineProfile(0, List.of(up, down)));

            // 1470.00 x 1.10 x 0.90 = 1455.30
            assertThat(amountFor(dataset.actuals(), 2026, 4)).isEqualTo(new BigDecimal("1455.30"));
        }

        @Test
        void randomNoiseStaysWithinTheConfiguredBand() {
            var tenYears = IntStream.rangeClosed(2030, 2039)
                    .mapToObj(year -> new PlanYear(year, BigDecimal.ONE, 12))
                    .toList();
            DemoDataset dataset = generator.generate(singleLineProfile(500, List.of(), tenYears, 7L));

            assertThat(dataset.actuals()).hasSize(120).allSatisfy(actual -> {
                BigDecimal budget = amountFor(dataset.budgets(), actual.period().year(), actual.period().month());
                // ±5% band, plus one cent of rounding tolerance
                BigDecimal tolerance = budget.multiply(new BigDecimal("0.05")).add(new BigDecimal("0.01"));
                assertThat(actual.amount().subtract(budget).abs()).isLessThanOrEqualTo(tolerance);
            });
        }
    }

    @Nested
    class Determinism {

        @Test
        void sameProfileAlwaysProducesTheSameDataset() {
            DemoCompanyProfile profile = AceJanitorialProfile.create();

            assertThat(generator.generate(profile)).isEqualTo(generator.generate(profile));
        }

        @Test
        void differentSeedProducesDifferentActuals() {
            var years = List.of(new PlanYear(2026, BigDecimal.ONE, 12));
            DemoDataset first = generator.generate(singleLineProfile(500, List.of(), years, 1L));
            DemoDataset second = generator.generate(singleLineProfile(500, List.of(), years, 2L));

            assertThat(first.actuals()).isNotEqualTo(second.actuals());
        }
    }

    @Nested
    class AceJanitorialDemoCompany {

        private final DemoDataset dataset = generator.generate(AceJanitorialProfile.create());

        @Test
        void hasFullBudgetsForBothYearsAndActualsThroughSeptember2026() {
            int linePlans = AceJanitorialProfile.create().linePlans().size();

            assertThat(dataset.budgets()).hasSize(linePlans * 12 * 2);
            assertThat(dataset.actuals()).hasSize(linePlans * (12 + 9));
            assertThat(dataset.actuals())
                    .filteredOn(a -> a.period().year() == 2026)
                    .allSatisfy(a -> assertThat(a.period().month()).isLessThanOrEqualTo(9));
        }

        @Test
        void everyAmountIsPositiveWithExactlyTwoDecimalPlaces() {
            assertThat(dataset.budgets()).allSatisfy(b -> {
                assertThat(b.amount().scale()).isEqualTo(2);
                assertThat(b.amount()).isPositive();
            });
            assertThat(dataset.actuals()).allSatisfy(a -> {
                assertThat(a.amount().scale()).isEqualTo(2);
                assertThat(a.amount()).isPositive();
            });
        }

        @Test
        void fuelSpikePushesVehicleCostsOverBudgetFromAprilToSeptember2026() {
            for (int month = 4; month <= 9; month++) {
                assertThat(amount(dataset.actuals(), AceJanitorialProfile.VEHICLE_AND_FUEL, 2026, month))
                        .isGreaterThan(amount(dataset.budgets(), AceJanitorialProfile.VEHICLE_AND_FUEL, 2026, month));
            }
        }

        @Test
        void lostContractPushesContractRevenueUnderBudgetFromJulyToSeptember2026() {
            for (int month = 7; month <= 9; month++) {
                assertThat(amount(dataset.actuals(), AceJanitorialProfile.CONTRACT_REVENUE, 2026, month))
                        .isLessThan(amount(dataset.budgets(), AceJanitorialProfile.CONTRACT_REVENUE, 2026, month));
            }
        }

        private BigDecimal amount(List<SeedAmount> amounts, String accountCode, int year, int month) {
            return amounts.stream()
                    .filter(a -> a.accountCode().equals(accountCode))
                    .filter(a -> a.period().equals(FiscalPeriod.of(year, month)))
                    .findFirst()
                    .orElseThrow()
                    .amount();
        }
    }
}
