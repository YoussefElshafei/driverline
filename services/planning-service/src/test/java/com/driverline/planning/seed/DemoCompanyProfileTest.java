package com.driverline.planning.seed;

import static com.driverline.planning.account.AccountType.EXPENSE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.seed.DemoCompanyProfile.AccountSeed;
import com.driverline.planning.seed.DemoCompanyProfile.DepartmentSeed;
import com.driverline.planning.seed.DemoCompanyProfile.LinePlan;
import com.driverline.planning.seed.DemoCompanyProfile.PlanYear;
import com.driverline.planning.seed.DemoCompanyProfile.StoryEvent;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DemoCompanyProfileTest {

    private static final List<DepartmentSeed> DEPARTMENTS = List.of(new DepartmentSeed("OPS", "Operations"));
    private static final List<AccountSeed> ACCOUNTS = List.of(new AccountSeed("5300", "Fuel", EXPENSE));
    private static final List<PlanYear> YEARS = List.of(new PlanYear(2026, BigDecimal.ONE, 12));

    @Test
    void rejectsLinePlanWithUnknownAccountCode() {
        var plans = List.of(new LinePlan("9999", "OPS", BigDecimal.TEN, Seasonality.FLAT, 0));

        assertThatThrownBy(() -> new DemoCompanyProfile("Test Co", DEPARTMENTS, ACCOUNTS, plans, YEARS, List.of(), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("account code")
                .hasMessageContaining("9999");
    }

    @Test
    void rejectsLinePlanWithUnknownDepartmentCode() {
        var plans = List.of(new LinePlan("5300", "NOPE", BigDecimal.TEN, Seasonality.FLAT, 0));

        assertThatThrownBy(() -> new DemoCompanyProfile("Test Co", DEPARTMENTS, ACCOUNTS, plans, YEARS, List.of(), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("department code");
    }

    @Test
    void rejectsStoryEventForUnknownAccount() {
        var events = List.of(new StoryEvent("9999", 2026, 1, 2, BigDecimal.TEN, "Typo"));

        assertThatThrownBy(() -> new DemoCompanyProfile("Test Co", DEPARTMENTS, ACCOUNTS, List.of(), YEARS, events, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9999");
    }

    @Test
    void rejectsStoryEventWhoseMonthsAreOutOfOrder() {
        assertThatThrownBy(() -> new StoryEvent("5300", 2026, 6, 3, BigDecimal.ONE, "Backwards"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsLastActualMonthAboveTwelve() {
        assertThatThrownBy(() -> new PlanYear(2026, BigDecimal.ONE, 13)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeNoise() {
        assertThatThrownBy(() -> new LinePlan("5300", "OPS", BigDecimal.TEN, Seasonality.FLAT, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
