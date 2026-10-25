package com.driverline.planning.seed;

import static com.driverline.planning.account.AccountType.EXPENSE;
import static com.driverline.planning.account.AccountType.REVENUE;

import com.driverline.planning.seed.DemoCompanyProfile.AccountSeed;
import com.driverline.planning.seed.DemoCompanyProfile.DepartmentSeed;
import com.driverline.planning.seed.DemoCompanyProfile.LinePlan;
import com.driverline.planning.seed.DemoCompanyProfile.PlanYear;
import com.driverline.planning.seed.DemoCompanyProfile.StoryEvent;
import java.math.BigDecimal;
import java.util.List;

/**
 * Ace Janitorial Service: a fictional commercial janitorial company with about 50 employees
 * (around 40 hourly cleaners and 10 salaried staff) and roughly $4M in annual revenue.
 * Fiscal year = calendar year. FY2025 is closed; FY2026 has actuals through September.
 */
public final class AceJanitorialProfile {

    static final String SALES = "SALES";
    static final String OPERATIONS = "OPS";
    static final String FLEET = "FLEET";
    static final String ADMIN = "ADMIN";

    static final String CONTRACT_REVENUE = "4000";
    static final String SPECIALTY_REVENUE = "4100";
    static final String CLEANING_WAGES = "5000";
    static final String SALARIES = "5100";
    static final String CLEANING_SUPPLIES = "5200";
    static final String VEHICLE_AND_FUEL = "5300";
    static final String EQUIPMENT_MAINTENANCE = "5400";
    static final String RENT = "6000";
    static final String MARKETING = "6100";
    static final String INSURANCE = "6200";
    static final String SOFTWARE_AND_IT = "6300";
    static final String UTILITIES = "6400";

    private static final long RANDOM_SEED = 20_250_101L;

    private AceJanitorialProfile() {
    }

    public static DemoCompanyProfile create() {
        return new DemoCompanyProfile(
                "Ace Janitorial Service",
                departments(),
                accounts(),
                linePlans(),
                years(),
                storyEvents(),
                RANDOM_SEED);
    }

    private static List<DepartmentSeed> departments() {
        return List.of(
                new DepartmentSeed(SALES, "Sales & Account Management"),
                new DepartmentSeed(OPERATIONS, "Operations"),
                new DepartmentSeed(FLEET, "Fleet & Equipment"),
                new DepartmentSeed(ADMIN, "Administration"));
    }

    private static List<AccountSeed> accounts() {
        return List.of(
                new AccountSeed(CONTRACT_REVENUE, "Contract Cleaning Revenue", REVENUE),
                new AccountSeed(SPECIALTY_REVENUE, "Specialty Services Revenue", REVENUE),
                new AccountSeed(CLEANING_WAGES, "Wages - Cleaning Staff", EXPENSE),
                new AccountSeed(SALARIES, "Salaries", EXPENSE),
                new AccountSeed(CLEANING_SUPPLIES, "Cleaning Supplies", EXPENSE),
                new AccountSeed(VEHICLE_AND_FUEL, "Vehicle & Fuel", EXPENSE),
                new AccountSeed(EQUIPMENT_MAINTENANCE, "Equipment Maintenance", EXPENSE),
                new AccountSeed(RENT, "Rent", EXPENSE),
                new AccountSeed(MARKETING, "Marketing", EXPENSE),
                new AccountSeed(INSURANCE, "Insurance", EXPENSE),
                new AccountSeed(SOFTWARE_AND_IT, "Software & IT", EXPENSE),
                new AccountSeed(UTILITIES, "Utilities", EXPENSE));
    }

    private static List<LinePlan> linePlans() {
        return List.of(
                line(CONTRACT_REVENUE, SALES, "290000.00", Seasonality.CONTRACT, 150),
                line(SPECIALTY_REVENUE, SALES, "50000.00", Seasonality.SPECIALTY, 1200),
                line(SALARIES, SALES, "16000.00", Seasonality.FLAT, 100),
                line(MARKETING, SALES, "6000.00", Seasonality.MARKETING, 800),

                line(CLEANING_WAGES, OPERATIONS, "152000.00", Seasonality.LABOUR, 300),
                line(SALARIES, OPERATIONS, "28000.00", Seasonality.FLAT, 100),
                line(CLEANING_SUPPLIES, OPERATIONS, "24000.00", Seasonality.SUPPLIES, 600),

                line(SALARIES, FLEET, "7000.00", Seasonality.FLAT, 100),
                line(VEHICLE_AND_FUEL, FLEET, "15000.00", Seasonality.VEHICLE, 500),
                line(EQUIPMENT_MAINTENANCE, FLEET, "6000.00", Seasonality.FLAT, 1500),

                line(SALARIES, ADMIN, "26000.00", Seasonality.FLAT, 100),
                line(RENT, ADMIN, "12000.00", Seasonality.FLAT, 0),
                line(INSURANCE, ADMIN, "11000.00", Seasonality.FLAT, 0),
                line(SOFTWARE_AND_IT, ADMIN, "3200.00", Seasonality.FLAT, 300),
                line(UTILITIES, ADMIN, "2600.00", Seasonality.UTILITIES, 500));
    }

    private static List<PlanYear> years() {
        return List.of(
                new PlanYear(2025, new BigDecimal("1.00"), 12),
                new PlanYear(2026, new BigDecimal("1.05"), 9));
    }

    private static List<StoryEvent> storyEvents() {
        return List.of(
                new StoryEvent(SPECIALTY_REVENUE, 2025, 5, 5, new BigDecimal("1.30"),
                        "Large post-construction cleanup at a new office complex"),
                new StoryEvent(CLEANING_WAGES, 2025, 11, 12, new BigDecimal("1.08"),
                        "Flu season: overtime to cover crew sick calls"),
                new StoryEvent(CLEANING_SUPPLIES, 2026, 2, 9, new BigDecimal("1.07"),
                        "Main supplier raised chemical and paper prices"),
                new StoryEvent(MARKETING, 2026, 3, 3, new BigDecimal("0.55"),
                        "Spring campaign launch slipped from March to April"),
                new StoryEvent(MARKETING, 2026, 4, 4, new BigDecimal("1.40"),
                        "Delayed spring campaign launched in April"),
                new StoryEvent(VEHICLE_AND_FUEL, 2026, 4, 9, new BigDecimal("1.18"),
                        "Regional fuel price spike"),
                new StoryEvent(CONTRACT_REVENUE, 2026, 7, 9, new BigDecimal("0.94"),
                        "Lost a large office-tower contract at renewal"));
    }

    private static LinePlan line(
            String accountCode, String departmentCode, String monthlyBase, Seasonality seasonality, int noiseBp) {
        return new LinePlan(accountCode, departmentCode, new BigDecimal(monthlyBase), seasonality, noiseBp);
    }
}
