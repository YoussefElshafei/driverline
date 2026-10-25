package com.driverline.planning.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.TestcontainersConfiguration;
import com.driverline.planning.account.AccountRepository;
import com.driverline.planning.actual.ActualLineRepository;
import com.driverline.planning.budget.BudgetLine;
import com.driverline.planning.budget.BudgetLineRepository;
import com.driverline.planning.department.DepartmentRepository;
import com.driverline.planning.forecast.ForecastLineRepository;
import com.driverline.planning.period.FiscalPeriod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** The full application starts with seeding enabled (the default), so the demo company is loaded before tests run. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class DemoDataLoaderIT {

    private static final int LINE_PLANS = AceJanitorialProfile.create().linePlans().size();

    @Autowired
    private DemoDataLoader loader;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BudgetLineRepository budgetLineRepository;

    @Autowired
    private ActualLineRepository actualLineRepository;

    @Test
    void startupLoadsTheAceJanitorialDemoCompany() {
        assertThat(departmentRepository.count()).isEqualTo(4);
        assertThat(accountRepository.count()).isEqualTo(12);
        assertThat(budgetLineRepository.count()).isEqualTo(LINE_PLANS * 12L * 2);
        assertThat(actualLineRepository.count()).isEqualTo(LINE_PLANS * (12L + 9));
    }

    @Test
    void loadingAgainDoesNothingWhenDataAlreadyExists() {
        long budgetLinesBefore = budgetLineRepository.count();

        boolean loaded = loader.loadIfEmpty(AceJanitorialProfile.create());

        assertThat(loaded).isFalse();
        assertThat(budgetLineRepository.count()).isEqualTo(budgetLinesBefore);
    }

    @Test
    void savedBudgetMatchesTheGeneratorFormula() {
        // Contract revenue, January 2025: 290000.00 x 0.99 (CONTRACT seasonality) x 1.00 (growth) = 287100.00
        BudgetLine januaryContractRevenue = budgetLineRepository.findByPeriodYear(2025).stream()
                .filter(line -> line.getAccount().getCode().equals(AceJanitorialProfile.CONTRACT_REVENUE))
                .filter(line -> line.getPeriod().equals(FiscalPeriod.of(2025, 1)))
                .findFirst()
                .orElseThrow();

        assertThat(januaryContractRevenue.getAmount()).isEqualByComparingTo("287100.00");
        assertThat(januaryContractRevenue.getDepartment().getCode()).isEqualTo(AceJanitorialProfile.SALES);
    }

    @Test
    void startupAlsoLoadsTheFy2026ForecastDriversOnce(@Autowired ForecastLineRepository forecastLineRepository,
            @Autowired DemoForecastLoader forecastLoader) {
        assertThat(forecastLineRepository.countByFiscalYear(AceJanitorialForecastDrivers.YEAR))
                .isEqualTo(AceJanitorialForecastDrivers.drivers().size());

        assertThat(forecastLoader.loadIfMissing(AceJanitorialForecastDrivers.YEAR, AceJanitorialForecastDrivers.drivers()))
                .isFalse();
    }

    @Test
    void noActualsExistAfterSeptember2026() {
        assertThat(actualLineRepository.findByPeriodYear(2026))
                .allSatisfy(line -> assertThat(line.getPeriod().month()).isLessThanOrEqualTo(9));
    }
}
