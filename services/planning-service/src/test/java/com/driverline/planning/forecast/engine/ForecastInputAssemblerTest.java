package com.driverline.planning.forecast.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountType;
import com.driverline.planning.department.Department;
import com.driverline.planning.forecast.ForecastLine;
import com.driverline.planning.forecast.ForecastMethod;
import com.driverline.planning.forecast.Scenario;
import com.driverline.planning.variance.report.PlanningFact;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ForecastInputAssemblerTest {

    private final ForecastInputAssembler assembler = new ForecastInputAssembler();

    private final Account fuel = new Account("5300", "Vehicle & Fuel", AccountType.EXPENSE);
    private final Account rent = new Account("6000", "Rent", AccountType.EXPENSE);
    private final Department fleet = new Department("FLEET", "Fleet & Equipment");
    private final Department admin = new Department("ADMIN", "Administration");

    /** Scenario is read-only reference data with no public constructor, so tests use a mock with a code. */
    private static Scenario scenario(String code) {
        Scenario scenario = mock(Scenario.class);
        when(scenario.getCode()).thenReturn(code);
        return scenario;
    }

    private static PlanningFact fact(String account, String department, int month, String budget, String actual) {
        return new PlanningFact(account, "Account " + account, AccountType.EXPENSE, department, "Dept " + department,
                month, new BigDecimal(budget), actual == null ? null : new BigDecimal(actual));
    }

    private ForecastLine fuelDrivers() {
        ForecastLine line = new ForecastLine(2026, fuel, fleet, ForecastMethod.UNITS_X_RATE, "Vans", "Cost per van");
        line.setValues(scenario(Scenario.BASE), new BigDecimal("14"), new BigDecimal("1250.00"), null);
        line.setValues(scenario(Scenario.BEST), new BigDecimal("14"), new BigDecimal("1150.00"), null);
        return line;
    }

    @Test
    void linesWithoutConfigurationUseTheirBudget() {
        List<ForecastInputLine> lines = assembler.assemble(
                List.of(fact("6000", "ADMIN", 1, "12600.00", "12600.00")), List.of(), Scenario.BASE);

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.driver()).isEqualTo(DriverSpec.USE_BUDGET);
            assertThat(line.budgetByMonth()).containsEntry(1, new BigDecimal("12600.00"));
            assertThat(line.actualByMonth()).containsEntry(1, new BigDecimal("12600.00"));
        });
    }

    @Test
    void usesTheDriverValuesOfTheRequestedScenario() {
        List<PlanningFact> facts = List.of(fact("5300", "FLEET", 10, "15750.00", null));

        DriverSpec base = assembler.assemble(facts, List.of(fuelDrivers()), Scenario.BASE).getFirst().driver();
        DriverSpec best = assembler.assemble(facts, List.of(fuelDrivers()), Scenario.BEST).getFirst().driver();

        assertThat(base).isEqualTo(DriverSpec.unitsTimesRate(new BigDecimal("14"), new BigDecimal("1250.00")));
        assertThat(best.rate()).isEqualTo(new BigDecimal("1150.00"));
    }

    @Test
    void fallsBackToBudgetWhenAScenarioHasNoValues() {
        DriverSpec worst = assembler.assemble(
                List.of(fact("5300", "FLEET", 10, "15750.00", null)), List.of(fuelDrivers()), Scenario.WORST)
                .getFirst().driver();

        assertThat(worst).isEqualTo(DriverSpec.USE_BUDGET);
    }

    @Test
    void includesConfiguredLinesThatHaveNoBudgetOrActualsYet() {
        List<ForecastInputLine> lines = assembler.assemble(List.of(), List.of(fuelDrivers()), Scenario.BASE);

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.accountName()).isEqualTo("Vehicle & Fuel");
            assertThat(line.departmentName()).isEqualTo("Fleet & Equipment");
            assertThat(line.budgetByMonth()).isEmpty();
        });
    }

    @Test
    void ordersLinesByDepartmentThenAccount() {
        List<ForecastInputLine> lines = assembler.assemble(List.of(
                fact("5300", "FLEET", 1, "1.00", null),
                fact("6000", "ADMIN", 1, "1.00", null),
                fact("5100", "FLEET", 1, "1.00", null)), List.of(), Scenario.BASE);

        assertThat(lines).extracting(line -> line.departmentCode() + "/" + line.accountCode())
                .containsExactly("ADMIN/6000", "FLEET/5100", "FLEET/5300");
    }

    @Test
    void budgetMethodLinesIgnoreAnyStoredValues() {
        ForecastLine rentLine = new ForecastLine(2026, rent, admin, ForecastMethod.BUDGET, null, null);

        DriverSpec driver = assembler.assemble(List.of(), List.of(rentLine), Scenario.BASE).getFirst().driver();

        assertThat(driver).isEqualTo(DriverSpec.USE_BUDGET);
    }
}
