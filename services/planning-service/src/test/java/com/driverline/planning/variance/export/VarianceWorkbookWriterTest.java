package com.driverline.planning.variance.export;

import static com.driverline.planning.variance.export.ExcelRow.Kind.DETAIL;
import static com.driverline.planning.variance.export.ExcelRow.Kind.SUBTOTAL;
import static com.driverline.planning.variance.export.ExcelRow.Kind.TOTAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.variance.VarianceLine;
import com.driverline.planning.variance.VarianceCalculator;
import com.driverline.planning.variance.VarianceDirection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Writes a small report, opens the bytes again with POI, and checks what a user would see in Excel. */
class VarianceWorkbookWriterTest {

    // Title, 2 subtitles, blank row -> header on row index 4, data from index 5.
    private static final int HEADER_ROW = 4;
    private static final int OPS_ROW = 5;
    private static final int SUPPLIES_ROW = 6;
    private static final int FUEL_ROW = 7;
    private static final int OCTOBER_ROW = 8;
    private static final int TOTAL_ROW = 9;

    private XSSFWorkbook workbook;
    private XSSFSheet sheet;

    private static VarianceLine line(String budget, String actual, VarianceDirection direction) {
        return VarianceCalculator.calculate(new BigDecimal(budget), new BigDecimal(actual), direction);
    }

    @BeforeEach
    void writeAndReopen() throws IOException {
        ExcelReport report = new ExcelReport(
                "test.xlsx",
                "By department",
                "Budget vs. actual by department",
                List.of("FY2026, January to September", "Actuals recorded through September."),
                "Department / account",
                List.of(
                        ExcelRow.of("Operations – net contribution", SUBTOTAL, 0,
                                line("-1000.00", "-1025.00", VarianceDirection.HIGHER_IS_BETTER)),
                        ExcelRow.of("5200 Cleaning Supplies", DETAIL, 1, line("1000.00", "950.00", VarianceDirection.LOWER_IS_BETTER)),
                        ExcelRow.of("5300 Vehicle & Fuel", DETAIL, 1, line("0.00", "75.00", VarianceDirection.LOWER_IS_BETTER)),
                        ExcelRow.notClosed("October", new BigDecimal("400.00")),
                        ExcelRow.of("Net income", TOTAL, 0, line("800.00", "855.00", VarianceDirection.HIGHER_IS_BETTER))));

        byte[] bytes = new VarianceWorkbookWriter().write(report);
        workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes));
        sheet = workbook.getSheet("By department");
    }

    @AfterEach
    void close() throws IOException {
        workbook.close();
    }

    private Cell cell(int row, int column) {
        return sheet.getRow(row).getCell(column);
    }

    @Test
    void startsWithTheTitleSubtitlesAndColumnHeaders() {
        assertThat(cell(0, 0).getStringCellValue()).isEqualTo("Budget vs. actual by department");
        assertThat(cell(1, 0).getStringCellValue()).isEqualTo("FY2026, January to September");
        Row header = sheet.getRow(HEADER_ROW);
        assertThat(header.getCell(0).getStringCellValue()).isEqualTo("Department / account");
        assertThat(header.getCell(1).getStringCellValue()).isEqualTo("Budget");
        assertThat(header.getCell(5).getStringCellValue()).isEqualTo("Status");
    }

    @Test
    void writesBudgetAndActualAsNumbersWithAnAccountingFormat() {
        assertThat(cell(SUPPLIES_ROW, 1).getNumericCellValue()).isEqualTo(1000.00);
        assertThat(cell(SUPPLIES_ROW, 2).getNumericCellValue()).isEqualTo(950.00);
        assertThat(cell(SUPPLIES_ROW, 1).getCellStyle().getDataFormatString()).isEqualTo("#,##0.00;(#,##0.00)");
    }

    @Test
    void writesVarianceAsLiveFormulasWithCalculatedValuesStored() {
        int excelRow = SUPPLIES_ROW + 1;
        Cell variance = cell(SUPPLIES_ROW, 3);
        Cell percent = cell(SUPPLIES_ROW, 4);

        assertThat(variance.getCellType()).isEqualTo(CellType.FORMULA);
        assertThat(variance.getCellFormula()).isEqualTo("ROUND(C%d-B%d,2)".formatted(excelRow, excelRow));
        assertThat(variance.getNumericCellValue()).isEqualTo(-50.00);
        assertThat(percent.getNumericCellValue()).isEqualTo(-0.05);
        assertThat(percent.getCellStyle().getDataFormatString()).isEqualTo("0.0%;(0.0%)");
    }

    @Test
    void showsNaForThePercentageWhenTheBudgetIsZero() {
        Cell percent = cell(FUEL_ROW, 4);

        assertThat(percent.getCachedFormulaResultType()).isEqualTo(CellType.STRING);
        assertThat(percent.getStringCellValue()).isEqualTo("n/a");
    }

    @Test
    void colourCodesTheStatusColumn() {
        assertThat(cell(SUPPLIES_ROW, 5).getStringCellValue()).isEqualTo("Favorable");
        assertThat(cell(SUPPLIES_ROW, 5).getCellStyle().getFillForegroundColor())
                .isEqualTo(IndexedColors.LIGHT_GREEN.getIndex());
        assertThat(cell(FUEL_ROW, 5).getStringCellValue()).isEqualTo("Unfavorable");
        assertThat(cell(FUEL_ROW, 5).getCellStyle().getFillForegroundColor())
                .isEqualTo(IndexedColors.ROSE.getIndex());
    }

    @Test
    void groupsAccountRowsUnderTheirDepartmentSoExcelCanCollapseThem() {
        assertThat(sheet.getRow(OPS_ROW).getOutlineLevel()).isZero();
        assertThat(sheet.getRow(SUPPLIES_ROW).getOutlineLevel()).isEqualTo(1);
        assertThat(sheet.getRow(FUEL_ROW).getOutlineLevel()).isEqualTo(1);
        assertThat(sheet.getRow(OCTOBER_ROW).getOutlineLevel()).isZero();
    }

    @Test
    void leavesVarianceEmptyForAMonthThatIsNotClosed() {
        assertThat(cell(OCTOBER_ROW, 1).getNumericCellValue()).isEqualTo(400.00);
        assertThat(sheet.getRow(OCTOBER_ROW).getCell(2)).isNull();
        assertThat(sheet.getRow(OCTOBER_ROW).getCell(3)).isNull();
        assertThat(cell(OCTOBER_ROW, 5).getStringCellValue()).isEqualTo("Not closed");
    }

    @Test
    void freezesTheHeaderRowAndTheLabelColumn() {
        assertThat(sheet.getPaneInformation().getHorizontalSplitPosition()).isEqualTo((short) (HEADER_ROW + 1));
        assertThat(sheet.getPaneInformation().getVerticalSplitPosition()).isEqualTo((short) 1);
    }

    @Test
    void makesTotalsBold() {
        assertThat(workbook.getFontAt(cell(TOTAL_ROW, 0).getCellStyle().getFontIndex()).getBold()).isTrue();
        assertThat(cell(TOTAL_ROW, 5).getStringCellValue()).isEqualTo("Favorable");
    }
}
