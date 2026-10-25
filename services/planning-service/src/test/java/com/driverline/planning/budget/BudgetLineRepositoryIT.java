package com.driverline.planning.budget;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.TestcontainersConfiguration;
import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountType;
import com.driverline.planning.department.Department;
import com.driverline.planning.period.FiscalPeriod;
import java.math.BigDecimal;
import java.util.List;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class BudgetLineRepositoryIT {

    @Autowired
    private BudgetLineRepository budgetLineRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Account fuel;
    private Department fleet;

    @BeforeEach
    void createReferenceData() {
        fuel = entityManager.persist(new Account("5300", "Vehicle & Fuel", AccountType.EXPENSE));
        fleet = entityManager.persist(new Department("FLEET", "Fleet & Equipment"));
    }

    @Test
    void storesMoneyExactlyWithoutRoundingDrift() {
        BudgetLine saved = budgetLineRepository.saveAndFlush(
                new BudgetLine(fuel, fleet, FiscalPeriod.of(2026, 4), new BigDecimal("1234567.89")));
        entityManager.clear();

        BudgetLine reloaded = budgetLineRepository.findById(saved.getId()).orElseThrow();

        assertThat(reloaded.getAmount()).isEqualTo(new BigDecimal("1234567.89"));
        assertThat(reloaded.getPeriod()).isEqualTo(FiscalPeriod.of(2026, 4));
    }

    @Test
    void findByPeriodYearReturnsOnlyThatYearWithAccountAndDepartmentAlreadyLoaded() {
        budgetLineRepository.save(new BudgetLine(fuel, fleet, FiscalPeriod.of(2025, 12), new BigDecimal("100.00")));
        budgetLineRepository.save(new BudgetLine(fuel, fleet, FiscalPeriod.of(2026, 1), new BigDecimal("200.00")));
        entityManager.flush();
        entityManager.clear();

        List<BudgetLine> lines = budgetLineRepository.findByPeriodYear(2026);

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.getAmount()).isEqualByComparingTo("200.00");
            // The entity graph fetched these in the same query, so no lazy loading (and no N+1) is needed.
            assertThat(Hibernate.isInitialized(line.getAccount())).isTrue();
            assertThat(Hibernate.isInitialized(line.getDepartment())).isTrue();
        });
    }

    @Test
    void rejectsSecondBudgetForSameAccountDepartmentAndMonth() {
        budgetLineRepository.saveAndFlush(new BudgetLine(fuel, fleet, FiscalPeriod.of(2026, 4), BigDecimal.TEN));

        assertThatThrownBy(() -> budgetLineRepository.saveAndFlush(
                        new BudgetLine(fuel, fleet, FiscalPeriod.of(2026, 4), BigDecimal.ONE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsMonthThirteenEvenWhenJavaValidationIsBypassed() {
        entityManager.flush();

        assertThatThrownBy(() -> jdbcTemplate.update("""
                        INSERT INTO budget_line (account_id, department_id, fiscal_year, fiscal_month, amount)
                        VALUES (?, ?, 2026, 13, 10.00)
                        """, fuel.getId(), fleet.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
