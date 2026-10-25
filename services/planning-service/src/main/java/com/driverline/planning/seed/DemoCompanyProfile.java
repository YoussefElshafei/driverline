package com.driverline.planning.seed;

import com.driverline.planning.account.AccountType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Everything needed to generate a fictional company's budgets and actuals.
 * The constructor checks the profile is internally consistent, so a typo in an account code fails fast.
 */
public record DemoCompanyProfile(
        String companyName,
        List<DepartmentSeed> departments,
        List<AccountSeed> accounts,
        List<LinePlan> linePlans,
        List<PlanYear> years,
        List<StoryEvent> storyEvents,
        long randomSeed) {

    public record DepartmentSeed(String code, String name) {
    }

    public record AccountSeed(String code, String name, AccountType type) {
    }

    /**
     * A budgeted line: one account in one department.
     *
     * @param monthlyBase      average monthly amount before seasonality and growth
     * @param noiseBasisPoints how far actuals randomly drift from budget, in hundredths of a percent (300 = ±3%)
     */
    public record LinePlan(
            String accountCode,
            String departmentCode,
            BigDecimal monthlyBase,
            Seasonality seasonality,
            int noiseBasisPoints) {

        public LinePlan {
            if (noiseBasisPoints < 0) {
                throw new IllegalArgumentException("noiseBasisPoints cannot be negative");
            }
        }
    }

    /**
     * @param growthFactor     multiplier applied to every line's budget that year (1.05 = 5% growth)
     * @param lastActualMonth  last month with actuals: 12 for a closed year, 0 for a year with no actuals yet
     */
    public record PlanYear(int year, BigDecimal growthFactor, int lastActualMonth) {

        public PlanYear {
            if (lastActualMonth < 0 || lastActualMonth > 12) {
                throw new IllegalArgumentException("lastActualMonth must be between 0 and 12");
            }
        }
    }

    /**
     * A real-world event that pushes actuals away from budget, e.g. a fuel price spike.
     * These give the variance report and AI commentary something meaningful to explain.
     *
     * @param factor multiplier applied to actuals in the affected months (1.18 = 18% over)
     */
    public record StoryEvent(
            String accountCode, int year, int fromMonth, int toMonth, BigDecimal factor, String description) {

        public StoryEvent {
            if (fromMonth < 1 || toMonth > 12 || fromMonth > toMonth) {
                throw new IllegalArgumentException("Story event months must satisfy 1 <= fromMonth <= toMonth <= 12");
            }
        }

        public boolean applies(String accountCode, int year, int month) {
            return this.accountCode.equals(accountCode) && this.year == year && month >= fromMonth && month <= toMonth;
        }
    }

    public DemoCompanyProfile {
        departments = List.copyOf(departments);
        accounts = List.copyOf(accounts);
        linePlans = List.copyOf(linePlans);
        years = List.copyOf(years);
        storyEvents = List.copyOf(storyEvents);
        validateReferences(departments, accounts, linePlans, storyEvents);
    }

    private static void validateReferences(
            List<DepartmentSeed> departments,
            List<AccountSeed> accounts,
            List<LinePlan> linePlans,
            List<StoryEvent> storyEvents) {
        Set<String> departmentCodes = departments.stream().map(DepartmentSeed::code).collect(Collectors.toSet());
        Set<String> accountCodes = accounts.stream().map(AccountSeed::code).collect(Collectors.toSet());

        for (LinePlan plan : linePlans) {
            requireKnown(accountCodes, plan.accountCode(), "account");
            requireKnown(departmentCodes, plan.departmentCode(), "department");
        }
        for (StoryEvent event : storyEvents) {
            requireKnown(accountCodes, event.accountCode(), "account");
        }
    }

    private static void requireKnown(Set<String> knownCodes, String code, String kind) {
        if (!knownCodes.contains(code)) {
            throw new IllegalArgumentException("Unknown %s code in demo profile: %s".formatted(kind, code));
        }
    }
}
