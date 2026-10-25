package com.driverline.planning.seed;

import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountRepository;
import com.driverline.planning.actual.ActualLine;
import com.driverline.planning.actual.ActualLineRepository;
import com.driverline.planning.budget.BudgetLine;
import com.driverline.planning.budget.BudgetLineRepository;
import com.driverline.planning.department.Department;
import com.driverline.planning.department.DepartmentRepository;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Saves a generated demo dataset to the database, but only into an empty database. */
@Service
public class DemoDataLoader {

    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final SeedDataGenerator generator;
    private final DepartmentRepository departmentRepository;
    private final AccountRepository accountRepository;
    private final BudgetLineRepository budgetLineRepository;
    private final ActualLineRepository actualLineRepository;

    public DemoDataLoader(
            SeedDataGenerator generator,
            DepartmentRepository departmentRepository,
            AccountRepository accountRepository,
            BudgetLineRepository budgetLineRepository,
            ActualLineRepository actualLineRepository) {
        this.generator = generator;
        this.departmentRepository = departmentRepository;
        this.accountRepository = accountRepository;
        this.budgetLineRepository = budgetLineRepository;
        this.actualLineRepository = actualLineRepository;
    }

    /**
     * Loads the profile's data if no departments exist yet. Runs in one transaction,
     * so a failure part-way leaves the database empty rather than half-seeded.
     *
     * @return true if data was loaded, false if the database already had data
     */
    @Transactional
    public boolean loadIfEmpty(DemoCompanyProfile profile) {
        if (departmentRepository.count() > 0) {
            log.info("Demo data not loaded: database already contains departments");
            return false;
        }

        DemoDataset dataset = generator.generate(profile);

        Map<String, Department> departmentsByCode = departmentRepository
                .saveAll(dataset.departments().stream().map(d -> new Department(d.code(), d.name())).toList())
                .stream()
                .collect(Collectors.toMap(Department::getCode, Function.identity()));

        Map<String, Account> accountsByCode = accountRepository
                .saveAll(dataset.accounts().stream().map(a -> new Account(a.code(), a.name(), a.type())).toList())
                .stream()
                .collect(Collectors.toMap(Account::getCode, Function.identity()));

        budgetLineRepository.saveAll(dataset.budgets().stream()
                .map(b -> new BudgetLine(
                        accountsByCode.get(b.accountCode()),
                        departmentsByCode.get(b.departmentCode()),
                        b.period(),
                        b.amount()))
                .toList());

        actualLineRepository.saveAll(dataset.actuals().stream()
                .map(a -> new ActualLine(
                        accountsByCode.get(a.accountCode()),
                        departmentsByCode.get(a.departmentCode()),
                        a.period(),
                        a.amount()))
                .toList());

        log.info("Loaded demo data for {}: {} departments, {} accounts, {} budget lines, {} actual lines",
                profile.companyName(),
                dataset.departments().size(),
                dataset.accounts().size(),
                dataset.budgets().size(),
                dataset.actuals().size());
        return true;
    }
}
