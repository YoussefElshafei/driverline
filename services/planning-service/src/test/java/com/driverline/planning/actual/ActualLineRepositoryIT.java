package com.driverline.planning.actual;

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

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ActualLineRepositoryIT {

    @Autowired
    private ActualLineRepository actualLineRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Account revenue;
    private Department sales;

    @BeforeEach
    void createReferenceData() {
        revenue = entityManager.persist(new Account("4000", "Contract Cleaning Revenue", AccountType.REVENUE));
        sales = entityManager.persist(new Department("SALES", "Sales & Account Management"));
    }

    @Test
    void storesNegativeAmountsForCreditsAndRefunds() {
        ActualLine saved = actualLineRepository.saveAndFlush(
                new ActualLine(revenue, sales, FiscalPeriod.of(2026, 2), new BigDecimal("-1500.25")));
        entityManager.clear();

        assertThat(actualLineRepository.findById(saved.getId()).orElseThrow().getAmount())
                .isEqualTo(new BigDecimal("-1500.25"));
    }

    @Test
    void findByPeriodYearReturnsOnlyThatYearWithAccountAndDepartmentAlreadyLoaded() {
        actualLineRepository.save(new ActualLine(revenue, sales, FiscalPeriod.of(2025, 6), new BigDecimal("100.00")));
        actualLineRepository.save(new ActualLine(revenue, sales, FiscalPeriod.of(2026, 6), new BigDecimal("200.00")));
        entityManager.flush();
        entityManager.clear();

        List<ActualLine> lines = actualLineRepository.findByPeriodYear(2025);

        assertThat(lines).singleElement().satisfies(line -> {
            assertThat(line.getPeriod()).isEqualTo(FiscalPeriod.of(2025, 6));
            assertThat(Hibernate.isInitialized(line.getAccount())).isTrue();
            assertThat(Hibernate.isInitialized(line.getDepartment())).isTrue();
        });
    }

    @Test
    void rejectsSecondActualForSameAccountDepartmentAndMonth() {
        actualLineRepository.saveAndFlush(new ActualLine(revenue, sales, FiscalPeriod.of(2026, 3), BigDecimal.TEN));

        assertThatThrownBy(() -> actualLineRepository.saveAndFlush(
                        new ActualLine(revenue, sales, FiscalPeriod.of(2026, 3), BigDecimal.ONE)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
