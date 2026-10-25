package com.driverline.planning.forecast.export;

import static org.assertj.core.api.Assertions.assertThat;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.forecast.ForecastMethod;
import com.driverline.planning.forecast.dto.ForecastDepartment;
import com.driverline.planning.forecast.dto.ForecastLineRow;
import com.driverline.planning.forecast.dto.ForecastReport;
import com.driverline.planning.forecast.dto.ForecastSummary;
import com.driverline.planning.forecast.dto.MonthAmount;
import com.driverline.planning.forecast.dto.OutlookLine;
import com.driverline.planning.forecast.dto.ScenarioComparison;
import com.driverline.planning.forecast.dto.ScenarioComparison.ScenarioOutlook;
import com.driverline.planning.variance.VarianceCalculator;
import com.driverline.planning.variance.VarianceDirection;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Two lines with 100 a month (Jan-Sep actual, Oct-Dec forecast) so every cached formula result is easy to check. */
class ForecastWorkbookWriterTest {

    private XSSFWorkbook workbook;
    private Sheet forecast;

    private static OutlookLine outlook(String budget, String outlook, VarianceDirection direction) {
        return OutlookLine.from(VarianceCalculator.calculate(new BigDecimal(budget), new BigDecimal(outlook), direction));
    }

    private static List<MonthAmount> months(String amount) {
        return IntStream.rangeClosed(1, 12).mapToObj(m -> new MonthAmount(m, new BigDecimal(amount), m <= 9)).toList();
    }

    @BeforeEach
    void writeAndReopen() throws IOException {
        OutlookLine revenue = outlook("1300.00", "1200.00", VarianceDirection.HIGHER_IS_BETTER);
        OutlookLine expense = outlook("500.00", "600.00", VarianceDirection.LOWER_IS_BETTER);
        OutlookLine net = outlook("800.00", "600.00", VarianceDirection.HIGHER_IS_BETTER);
        ForecastSummary summary = new ForecastSummary(revenue, expense, net);
        ForecastReport report = new ForecastReport(2026, "BASE", "Base", 9, summary, List.of(
                new ForecastDepartment("SALES", "Sales & Account Management", net, List.of(
                        new ForecastLineRow("4000", "Contract Cleaning Revenue", AccountType.REVENUE,
                                ForecastMethod.UNITS_X_RATE, months("100.00"), revenue),
                        new ForecastLineRow("6100", "Marketing", AccountType.EXPENSE,
                                ForecastMethod.BUDGET, months("50.00"), expense)))));
        ScenarioComparison comparison = new ScenarioComparison(2026, 9, List.of(
                new ScenarioOutlook("BASE", "Base", summary),
                new ScenarioOutlook("BEST", "Best", new ForecastSummary(revenue, expense,
                        outlook("800.00", "900.00", VarianceDirection.HIGHER_IS_BETTER)))));

        workbook = new XSSFWorkbook(new ByteArrayInputStream(new ForecastWorkbookWriter().write(report, comparison)));
        forecast = workbook.getSheet("Forecast - Base");
    }

    @AfterEach
    void close() throws IOException {
        workbook.close();
    }

    private Row row(Sheet sheet, String label) {
        for (Row row : sheet) {
            Cell first = row.getCell(0);
            if (first != null && label.equals(first.getStringCellValue())) {
                return row;
            }
        }
        throw new AssertionError("No row labelled " + label);
    }

    @Test
    void labelsActualAndForecastMonthsAndShadesForecastColumns() {
        Row header = row(forecast, "Department / account");

        assertThat(header.getCell(9).getStringCellValue()).isEqualTo("Sep (A)");
        assertThat(header.getCell(10).getStringCellValue()).isEqualTo("Oct (F)");
        assertThat(header.getCell(10).getCellStyle().getFillForegroundColor()).isEqualTo(IndexedColors.PALE_BLUE.getIndex());

        Row contracts = row(forecast, "4000 Contract Cleaning Revenue");
        assertThat(contracts.getCell(10).getCellStyle().getFillForegroundColor()).isEqualTo(IndexedColors.PALE_BLUE.getIndex());
        assertThat(contracts.getCell(9).getCellStyle().getFillPattern().getCode()).isZero();
    }

    @Test
    void fullYearIsASumFormulaOverTheTwelveMonths() {
        Row contracts = row(forecast, "4000 Contract Cleaning Revenue");
        int excelRow = contracts.getRowNum() + 1;

        assertThat(contracts.getCell(13).getCellFormula()).isEqualTo("SUM(B%d:M%d)".formatted(excelRow, excelRow));
        assertThat(contracts.getCell(13).getNumericCellValue()).isEqualTo(1200.00);
        assertThat(contracts.getCell(14).getNumericCellValue()).isEqualTo(1300.00);
        assertThat(contracts.getCell(15).getNumericCellValue()).isEqualTo(-100.00);
        assertThat(contracts.getCell(17).getStringCellValue()).isEqualTo("Unfavorable");
    }

    @Test
    void totalsAreFormulasOverTheLinesAndNetIncomeIsRevenueMinusExpenses() {
        Row revenue = row(forecast, "Total revenue");
        Row netIncome = row(forecast, "Net income");
        int contractsRow = row(forecast, "4000 Contract Cleaning Revenue").getRowNum() + 1;

        assertThat(revenue.getCell(1).getCellFormula()).isEqualTo("SUM(B%d)".formatted(contractsRow));
        assertThat(netIncome.getCell(1).getCellFormula())
                .isEqualTo("B%d-B%d".formatted(revenue.getRowNum() + 1, revenue.getRowNum() + 2));
        assertThat(netIncome.getCell(1).getNumericCellValue()).isEqualTo(50.00);
        assertThat(netIncome.getCell(13).getNumericCellValue()).isEqualTo(600.00);
        assertThat(netIncome.getCell(14).getNumericCellValue()).isEqualTo(800.00);
    }

    @Test
    void groupsAccountRowsUnderTheirDepartment() {
        assertThat(row(forecast, "Sales & Account Management").getOutlineLevel()).isZero();
        assertThat(row(forecast, "4000 Contract Cleaning Revenue").getOutlineLevel()).isEqualTo(1);
        assertThat(row(forecast, "Total revenue").getOutlineLevel()).isZero();
    }

    @Test
    void comparesScenariosOnASecondSheet() {
        Sheet scenarios = workbook.getSheet("Scenarios");
        Row net = row(scenarios, "Net income");
        Row vsBudget = row(scenarios, "Net income vs. budget");

        assertThat(net.getCell(1).getNumericCellValue()).isEqualTo(800.00);
        assertThat(net.getCell(2).getNumericCellValue()).isEqualTo(600.00);
        assertThat(net.getCell(3).getNumericCellValue()).isEqualTo(900.00);
        assertThat(vsBudget.getCell(2).getNumericCellValue()).isEqualTo(-200.00);
        assertThat(vsBudget.getCell(3).getNumericCellValue()).isEqualTo(100.00);
    }
}
