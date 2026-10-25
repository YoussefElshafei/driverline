package com.driverline.planning.forecast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.account.Account;
import com.driverline.planning.account.AccountType;
import com.driverline.planning.common.error.FieldErrorDetail;
import com.driverline.planning.common.error.RequestValidationException;
import com.driverline.planning.department.Department;
import com.driverline.planning.forecast.dto.SaveDriversRequest.DriverLineRequest;
import com.driverline.planning.forecast.dto.SaveDriversRequest.DriverValuesRequest;
import com.driverline.planning.reference.ReferenceData;
import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class DriverRequestValidatorTest {

    private static final Set<String> SCENARIOS = new LinkedHashSet<>(List.of("BASE", "BEST", "WORST"));
    private static final ReferenceData REFERENCE_DATA = new ReferenceData(
            Map.of("4000", new Account("4000", "Contract Cleaning Revenue", AccountType.REVENUE),
                    "5200", new Account("5200", "Cleaning Supplies", AccountType.EXPENSE)),
            Map.of("OPS", new Department("OPS", "Operations"), "SALES", new Department("SALES", "Sales")));

    private final DriverRequestValidator validator = new DriverRequestValidator();

    private static DriverValuesRequest units(String quantity, String rate) {
        return new DriverValuesRequest(new BigDecimal(quantity), new BigDecimal(rate), null);
    }

    private static DriverValuesRequest percent(String percent) {
        return new DriverValuesRequest(null, null, new BigDecimal(percent));
    }

    private static List<FieldErrorDetail> errorsFor(DriverLineRequest... lines) {
        try {
            new DriverRequestValidator().validate(List.of(lines), REFERENCE_DATA, SCENARIOS);
            return List.of();
        } catch (RequestValidationException e) {
            return e.getErrors();
        }
    }

    @Test
    void acceptsCompleteUnitsTimesRateAndPercentAndBudgetLines() {
        assertThatCode(() -> validator.validate(List.of(
                new DriverLineRequest("4000", "SALES", ForecastMethod.UNITS_X_RATE, "Contracts", "Value",
                        Map.of("BASE", units("62", "4700"), "BEST", units("64", "4725"), "WORST", units("60", "4650"))),
                new DriverLineRequest("5200", "OPS", ForecastMethod.PERCENT_OF_REVENUE, null, null,
                        Map.of("BASE", percent("7.6"), "BEST", percent("7.2"), "WORST", percent("8"))),
                new DriverLineRequest("5200", "SALES", ForecastMethod.BUDGET, null, null, null)),
                REFERENCE_DATA, SCENARIOS)).doesNotThrowAnyException();
    }

    @Test
    void requiresQuantityAndRateForEveryScenario() {
        List<FieldErrorDetail> errors = errorsFor(new DriverLineRequest("4000", "SALES", ForecastMethod.UNITS_X_RATE,
                null, null, Map.of("BASE", units("62", "4700"), "BEST", new DriverValuesRequest(BigDecimal.ONE, null, null))));

        assertThat(errors).extracting(FieldErrorDetail::field).containsExactly(
                "lines[0].values.BEST.rate", "lines[0].values.WORST.quantity", "lines[0].values.WORST.rate");
    }

    @Test
    void rejectsARevenueLineAsAPercentageOfRevenue() {
        List<FieldErrorDetail> errors = errorsFor(new DriverLineRequest("4000", "SALES", ForecastMethod.PERCENT_OF_REVENUE,
                null, null, Map.of("BASE", percent("5"), "BEST", percent("5"), "WORST", percent("5"))));

        assertThat(errors).singleElement().satisfies(error -> {
            assertThat(error.field()).isEqualTo("lines[0].method");
            assertThat(error.message()).contains("circular");
        });
    }

    @Test
    void requiresAPercentForEveryScenarioOnPercentOfRevenueLines() {
        List<FieldErrorDetail> errors = errorsFor(new DriverLineRequest("5200", "OPS", ForecastMethod.PERCENT_OF_REVENUE,
                null, null, Map.of("BASE", percent("7.6"))));

        assertThat(errors).extracting(FieldErrorDetail::field)
                .containsExactly("lines[0].values.BEST.percent", "lines[0].values.WORST.percent");
    }

    @Test
    void reportsUnknownCodesScenariosAndDuplicateLines() {
        List<FieldErrorDetail> errors = errorsFor(
                new DriverLineRequest("9999", "NOPE", ForecastMethod.BUDGET, null, null, Map.of("MAYBE", percent("1"))),
                new DriverLineRequest("5200", "OPS", ForecastMethod.BUDGET, null, null, null),
                new DriverLineRequest("5200", "OPS", ForecastMethod.BUDGET, null, null, null));

        assertThat(errors).extracting(FieldErrorDetail::field).containsExactly(
                "lines[0].accountCode", "lines[0].departmentCode", "lines[0].values", "lines[2].accountCode");
        assertThat(errors.get(3).message()).contains("Duplicate of lines[1]");
    }

    @Test
    void failsWithTheNothingSavedMessage() {
        assertThatThrownBy(() -> validator.validate(List.of(
                new DriverLineRequest("9999", "OPS", ForecastMethod.BUDGET, null, null, null)), REFERENCE_DATA, SCENARIOS))
                .isInstanceOf(RequestValidationException.class)
                .hasMessage("Some drivers are invalid. Nothing was saved.");
    }
}
