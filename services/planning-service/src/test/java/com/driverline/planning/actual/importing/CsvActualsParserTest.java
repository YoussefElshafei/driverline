package com.driverline.planning.actual.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvActualsParserTest {

    private final CsvActualsParser parser = new CsvActualsParser();

    private List<RawActualRow> parse(String csv) {
        return parser.parse(new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void supportsOnlyCsvFilesCaseInsensitively() {
        assertThat(parser.supports("actuals.csv")).isTrue();
        assertThat(parser.supports("ACTUALS.CSV")).isTrue();
        assertThat(parser.supports("actuals.xlsx")).isFalse();
        assertThat(parser.supports(null)).isFalse();
    }

    @Test
    void readsRowsWithLineNumbersMatchingWhatTheUserSees() {
        List<RawActualRow> rows = parse("""
                year,month,account_code,department_code,amount
                2026,10,4000,SALES,301250.00
                2026,10,5300,FLEET,17890.45
                """);

        assertThat(rows).containsExactly(
                new RawActualRow(2, "2026", "10", "4000", "SALES", "301250.00"),
                new RawActualRow(3, "2026", "10", "5300", "FLEET", "17890.45"));
    }

    @Test
    void acceptsColumnsInAnyOrderAndHeadersInAnyCase() {
        List<RawActualRow> rows = parse("""
                Amount,Department_Code,ACCOUNT_CODE,Month,Year
                500.00,OPS,5200,3,2026
                """);

        assertThat(rows).singleElement().isEqualTo(new RawActualRow(2, "2026", "3", "5200", "OPS", "500.00"));
    }

    @Test
    void handlesQuotedAmountsContainingThousandsSeparators() {
        List<RawActualRow> rows = parse("""
                year,month,account_code,department_code,amount
                2026,10,4000,SALES,"301,250.00"
                """);

        assertThat(rows.getFirst().amount()).isEqualTo("301,250.00");
    }

    @Test
    void ignoresTheByteOrderMarkThatExcelAddsToUtf8Csv() {
        List<RawActualRow> rows = parse("﻿year,month,account_code,department_code,amount\n2026,1,4000,SALES,1.00\n");

        assertThat(rows).hasSize(1);
    }

    @Test
    void skipsBlankLinesButKeepsTrueLineNumbers() {
        List<RawActualRow> rows = parse("""
                year,month,account_code,department_code,amount
                2026,1,4000,SALES,1.00
                ,,,,
                2026,2,4000,SALES,2.00
                """);

        assertThat(rows).extracting(RawActualRow::rowNumber).containsExactly(2, 4);
    }

    @Test
    void shortRowsGiveBlankValuesForTheValidatorToReport() {
        List<RawActualRow> rows = parse("""
                year,month,account_code,department_code,amount
                2026,10,4000
                """);

        assertThat(rows.getFirst().departmentCode()).isEmpty();
        assertThat(rows.getFirst().amount()).isEmpty();
    }

    @Test
    void reportsEveryMissingRequiredColumn() {
        assertThatThrownBy(() -> parse("year,month,account\n2026,1,4000\n"))
                .isInstanceOfSatisfying(ImportValidationException.class, e -> assertThat(e.getErrors())
                        .extracting(RowError::column)
                        .containsExactly("account_code", "department_code", "amount"));
    }

    @Test
    void rejectsAnEmptyFile() {
        assertThatThrownBy(() -> parse(""))
                .isInstanceOf(ImportValidationException.class)
                .satisfies(e -> assertThat(((ImportValidationException) e).getErrors().getFirst().message())
                        .contains("empty"));
    }
}
