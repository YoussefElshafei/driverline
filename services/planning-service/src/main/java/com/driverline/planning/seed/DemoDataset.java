package com.driverline.planning.seed;

import com.driverline.planning.period.FiscalPeriod;
import com.driverline.planning.seed.DemoCompanyProfile.AccountSeed;
import com.driverline.planning.seed.DemoCompanyProfile.DepartmentSeed;
import java.math.BigDecimal;
import java.util.List;

/** Generated demo data, still independent of the database. Lines refer to accounts and departments by code. */
public record DemoDataset(
        List<DepartmentSeed> departments,
        List<AccountSeed> accounts,
        List<SeedAmount> budgets,
        List<SeedAmount> actuals) {

    public record SeedAmount(String accountCode, String departmentCode, FiscalPeriod period, BigDecimal amount) {
    }
}
