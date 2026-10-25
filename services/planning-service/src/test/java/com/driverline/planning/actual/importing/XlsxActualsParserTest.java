package com.driverline.planning.actual.importing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.driverline.planning.actual.importing.XlsxTestFiles.Formula;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class XlsxActualsParserTest {

    private static final List<Object> HEADER = List.of("year", "month", "account_code", "department_code", "amount");

    private final XlsxActualsParser parser = new XlsxActualsParser();

    private List<RawActualRow> parse(byte[] workbook) {
        return parser.parse(new ByteArrayInputStream(workbook));
    }

    @Test
    void supportsOnlyXlsxFiles() {
        assertThat(parser.supports("actuals.xlsx")).isTrue();
        assertThat(parser.supports("Actuals.XLSX")).isTrue();
        assertThat(parser.supports("old-format.xls")).isFalse();
        assertThat(parser.supports("actuals.csv")).isFalse();
    }

    @Test
    void readsNumericCellsWithoutFloatingPointNoise() {
        // 1234.56 is stored by Excel as a binary double close to, but not exactly, 1234.56.
        byte[] file = XlsxTestFiles.workbook(List.of(HEADER, List.of(2026, 10, 5300, "FLEET", 1234.56)));

        assertThat(parse(file)).singleElement()
                .isEqualTo(new RawActualRow(2, "2026", "10", "5300", "FLEET", "1234.56"));
    }

    @Test
    void readsTheCalculatedResultOfFormulaCells() {
        byte[] file = XlsxTestFiles.workbook(List.of(HEADER, List.of(2026, 10, "4000", "SALES", new Formula("100000+23.45"))));

        assertThat(parse(file).getFirst().amount()).isEqualTo("100023.45");
    }

    @Test
    void acceptsColumnsInAnyOrderWithMixedCaseHeaders() {
        byte[] file = XlsxTestFiles.workbook(List.of(
                List.of("AMOUNT", "Department_Code", "account_code", "Month", "Year"),
                List.of("99.95", "OPS", "5200", 4, 2026)));

        assertThat(parse(file)).singleElement()
                .isEqualTo(new RawActualRow(2, "2026", "4", "5200", "OPS", "99.95"));
    }

    @Test
    void skipsEmptyRowsButKeepsExcelRowNumbers() {
        byte[] file = XlsxTestFiles.workbook(Arrays.asList(
                HEADER,
                List.of(2026, 1, 4000, "SALES", 1),
                null,
                List.of(2026, 2, 4000, "SALES", 2)));

        assertThat(parse(file)).extracting(RawActualRow::rowNumber).containsExactly(2, 4);
    }

    @Test
    void missingCellsBecomeBlankValues() {
        byte[] file = XlsxTestFiles.workbook(List.of(HEADER, Arrays.asList(2026, 10, 4000, null, null)));

        assertThat(parse(file).getFirst().departmentCode()).isEmpty();
    }

    @Test
    void reportsMissingRequiredColumns() {
        byte[] file = XlsxTestFiles.workbook(List.of(List.of("year", "month", "amount"), List.of(2026, 1, 5)));

        assertThatThrownBy(() -> parse(file))
                .isInstanceOfSatisfying(ImportValidationException.class, e -> assertThat(e.getErrors())
                        .extracting(RowError::column)
                        .containsExactly("account_code", "department_code"));
    }

    @Test
    void rejectsAFileThatIsNotReallyAWorkbook() {
        byte[] notAWorkbook = "year,month\n2026,1\n".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> parse(notAWorkbook))
                .isInstanceOfSatisfying(ImportValidationException.class, e -> assertThat(e.getErrors().getFirst().message())
                        .contains("could not be read as an .xlsx workbook"));
    }

    @Test
    void rejectsAnEmptyWorksheet() {
        byte[] file = XlsxTestFiles.workbook(List.of());

        assertThatThrownBy(() -> parse(file)).isInstanceOf(ImportValidationException.class);
    }
}
