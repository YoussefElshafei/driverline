package com.driverline.planning.actual.importing;

import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.period.FiscalPeriod;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ActualRowValidatorTest {

    private static final Set<String> ACCOUNTS = Set.of("4000", "5300");
    private static final Set<String> DEPARTMENTS = Set.of("SALES", "FLEET");

    private final ActualRowValidator validator = new ActualRowValidator();

    private ActualRowValidator.Result validate(RawActualRow... rows) {
        return validator.validate(List.of(rows), ACCOUNTS, DEPARTMENTS);
    }

    private static RawActualRow row(int rowNumber, String year, String month, String account, String dept, String amount) {
        return new RawActualRow(rowNumber, year, month, account, dept, amount);
    }

    @Test
    void convertsAValidRowToTypedValues() {
        var result = validate(row(2, "2026", "10", "5300", "FLEET", "$17,890.45"));

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.validRows()).singleElement().isEqualTo(
                new ValidActualRow(2, "5300", "FLEET", FiscalPeriod.of(2026, 10), new BigDecimal("17890.45")));
    }

    @Test
    void acceptsLowerCaseDepartmentCodes() {
        var result = validate(row(2, "2026", "10", "5300", "fleet", "1.00"));

        assertThat(result.validRows().getFirst().departmentCode()).isEqualTo("FLEET");
    }

    @Test
    void reportsEveryProblemOnARowNotJustTheFirst() {
        var result = validate(row(5, "twenty", "13", "9999", "", "12.345"));

        assertThat(result.validRows()).isEmpty();
        assertThat(result.errors()).containsExactly(
                new RowError(5, "year", "twenty", "must be a whole number"),
                new RowError(5, "month", "13", "must be between 1 and 12"),
                new RowError(5, "account_code", "9999", "Unknown account code '9999'"),
                new RowError(5, "department_code", "", "is required"),
                new RowError(5, "amount", "12.345", "'12.345' has more than 2 decimal places"));
    }

    @Test
    void collectsErrorsAcrossAllRowsAndKeepsTheValidOnesSeparate() {
        var result = validate(
                row(2, "2026", "10", "4000", "SALES", "100.00"),
                row(3, "2026", "0", "4000", "SALES", "100.00"),
                row(4, "1999", "10", "4000", "SALES", "100.00"));

        assertThat(result.validRows()).extracting(ValidActualRow::rowNumber).containsExactly(2);
        assertThat(result.errors()).extracting(RowError::row).containsExactly(3, 4);
    }

    @Test
    void flagsDuplicateRowsForTheSameAccountDepartmentAndMonth() {
        var result = validate(
                row(2, "2026", "10", "4000", "SALES", "100.00"),
                row(7, "2026", "10", "4000", "sales", "250.00"));

        assertThat(result.errors()).singleElement().satisfies(error -> {
            assertThat(error.row()).isEqualTo(7);
            assertThat(error.message()).contains("Duplicate of row 2");
        });
    }

    @Test
    void allowsZeroAndNegativeAmountsForCreditsAndRefunds() {
        var result = validate(
                row(2, "2026", "10", "4000", "SALES", "0"),
                row(3, "2026", "11", "4000", "SALES", "(1,500.25)"));

        assertThat(result.hasErrors()).isFalse();
        assertThat(result.validRows()).extracting(ValidActualRow::amount)
                .containsExactly(new BigDecimal("0.00"), new BigDecimal("-1500.25"));
    }

    @Test
    void treatsWhitespaceOnlyValuesAsMissing() {
        var result = validate(row(2, "  ", "10", "4000", "SALES", "1.00"));

        assertThat(result.errors()).singleElement().extracting(RowError::message).isEqualTo("is required");
    }
}
