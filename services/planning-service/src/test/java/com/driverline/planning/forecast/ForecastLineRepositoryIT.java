package com.driverline.planning.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.TestcontainersConfiguration;
import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountType;
import com.driverline.planning.department.Department;
import java.math.BigDecimal;
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
class ForecastLineRepositoryIT {

    @Autowired
    private ForecastLineRepository forecastLineRepository;

    @Autowired
    private ScenarioRepository scenarioRepository;

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

    private ForecastLine fuelLineWithBaseAndBest() {
        ForecastLine line = new ForecastLine(2026, fuel, fleet, ForecastMethod.UNITS_X_RATE, "Vans", "Cost per van");
        line.setValues(scenarioRepository.findByCode(Scenario.BASE).orElseThrow(), new BigDecimal("14"), new BigDecimal("1250.00"), null);
        line.setValues(scenarioRepository.findByCode(Scenario.BEST).orElseThrow(), new BigDecimal("14"), new BigDecimal("1150.00"), null);
        return line;
    }

    @Test
    void theMigrationInsertsTheThreeScenariosInOrder() {
        assertThat(scenarioRepository.findAllByOrderBySortOrderAsc())
                .extracting(Scenario::getCode)
                .containsExactly("BASE", "BEST", "WORST");
    }

    @Test
    void savingALineSavesItsDriverValuesAndLoadsThemBackInOneQuery() {
        forecastLineRepository.saveAndFlush(fuelLineWithBaseAndBest());
        entityManager.clear();

        ForecastLine loaded = forecastLineRepository.findByFiscalYear(2026).getFirst();

        assertThat(Hibernate.isInitialized(loaded.getValues())).isTrue();
        assertThat(loaded.getValues()).hasSize(2);
        assertThat(loaded.valueFor(Scenario.BEST).orElseThrow().getRate()).isEqualTo(new BigDecimal("1150.00"));
        assertThat(loaded.getAccount().getCode()).isEqualTo("5300");
    }

    @Test
    void switchingToBudgetDeletesTheDriverValues() {
        ForecastLine saved = forecastLineRepository.saveAndFlush(fuelLineWithBaseAndBest());

        saved.changeMethod(ForecastMethod.BUDGET, null, null);
        forecastLineRepository.saveAndFlush(saved);

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM forecast_driver_value WHERE forecast_line_id = ?", Integer.class, saved.getId());
        assertThat(remaining).isZero();
    }

    @Test
    void updatingAScenarioReplacesItsValuesInsteadOfAddingAnotherRow() {
        ForecastLine saved = forecastLineRepository.saveAndFlush(fuelLineWithBaseAndBest());

        saved.setValues(scenarioRepository.findByCode(Scenario.BASE).orElseThrow(), new BigDecimal("15"), new BigDecimal("1300.00"), null);
        forecastLineRepository.saveAndFlush(saved);
        entityManager.clear();

        ForecastLine loaded = forecastLineRepository.findById(saved.getId()).orElseThrow();
        assertThat(loaded.getValues()).hasSize(2);
        assertThat(loaded.valueFor(Scenario.BASE).orElseThrow().getQuantity()).isEqualTo(new BigDecimal("15.00"));
    }

    @Test
    void allowsOnlyOneForecastLinePerAccountDepartmentAndYear() {
        forecastLineRepository.saveAndFlush(new ForecastLine(2026, fuel, fleet, ForecastMethod.BUDGET, null, null));

        assertThatThrownBy(() -> forecastLineRepository.saveAndFlush(
                        new ForecastLine(2026, fuel, fleet, ForecastMethod.BUDGET, null, null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void theDatabaseRejectsAPercentOverOneHundred() {
        ForecastLine saved = forecastLineRepository.saveAndFlush(
                new ForecastLine(2026, fuel, fleet, ForecastMethod.PERCENT_OF_REVENUE, null, null));
        Long baseId = scenarioRepository.findByCode(Scenario.BASE).orElseThrow().getId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                        "INSERT INTO forecast_driver_value (forecast_line_id, scenario_id, percent) VALUES (?, ?, 150)",
                        saved.getId(), baseId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
