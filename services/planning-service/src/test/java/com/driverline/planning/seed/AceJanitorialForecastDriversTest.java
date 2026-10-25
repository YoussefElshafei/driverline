package com.driverline.planning.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.ForecastMethod;
import com.driverline.planning.seed.AceJanitorialForecastDrivers.DriverSeed;
import com.driverline.planning.seed.AceJanitorialForecastDrivers.Values;
import com.driverline.planning.seed.DemoCompanyProfile.AccountSeed;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class AceJanitorialForecastDriversTest {

    private final DemoCompanyProfile profile = AceJanitorialProfile.create();
    private final List<DriverSeed> drivers = AceJanitorialForecastDrivers.drivers();

    @Test
    void everyDriverIsForALineThatTheDemoCompanyBudgets() {
        var budgetedLines = profile.linePlans().stream()
                .map(plan -> plan.accountCode() + "/" + plan.departmentCode())
                .collect(Collectors.toSet());

        assertThat(drivers).allSatisfy(driver ->
                assertThat(budgetedLines).contains(driver.accountCode() + "/" + driver.departmentCode()));
    }

    @Test
    void percentOfRevenueIsOnlyUsedForExpenses() {
        Map<String, AccountType> types = profile.accounts().stream()
                .collect(Collectors.toMap(AccountSeed::code, AccountSeed::type));

        assertThat(drivers)
                .filteredOn(driver -> driver.method() == ForecastMethod.PERCENT_OF_REVENUE)
                .isNotEmpty()
                .allSatisfy(driver -> assertThat(types.get(driver.accountCode())).isEqualTo(AccountType.EXPENSE));
    }

    @Test
    void everyScenarioHasTheValuesItsMethodNeeds() {
        assertThat(drivers).allSatisfy(driver -> Stream.of(driver.base(), driver.best(), driver.worst())
                .forEach((Values values) -> {
                    if (driver.method() == ForecastMethod.UNITS_X_RATE) {
                        assertThat(values.quantity()).isNotNull();
                        assertThat(values.rate()).isNotNull();
                    } else {
                        assertThat(values.percent()).isNotNull();
                    }
                }));
    }

    @Test
    void bestScenarioExpectsMoreContractRevenueThanWorst() {
        DriverSeed contracts = drivers.getFirst();

        assertThat(contracts.best().quantity().multiply(contracts.best().rate()))
                .isGreaterThan(contracts.base().quantity().multiply(contracts.base().rate()))
                .isGreaterThan(contracts.worst().quantity().multiply(contracts.worst().rate()));
    }
}
