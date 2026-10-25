package com.driverline.planning.forecast.export;

import static com.driverline.planning.common.excel.ExcelSheets.setFormula;
import static com.driverline.planning.common.excel.ExcelSheets.setNumber;
import static com.driverline.planning.common.excel.ExcelSheets.setText;

import com.driverline.planning.account.AccountType;
import com.driverline.planning.common.excel.ExcelSheets;
import com.driverline.planning.common.excel.ExcelStyles;
import com.driverline.planning.common.excel.ExcelStyles.RowStyles;
import com.driverline.planning.forecast.dto.ForecastDepartment;
import com.driverline.planning.forecast.dto.ForecastLineRow;
import com.driverline.planning.forecast.dto.ForecastReport;
import com.driverline.planning.forecast.dto.MonthAmount;
import com.driverline.planning.forecast.dto.OutlookLine;
import com.driverline.planning.forecast.dto.ScenarioComparison;
import com.driverline.planning.forecast.dto.ScenarioComparison.ScenarioOutlook;
import com.driverline.planning.forecast.dto.ForecastSummary;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Renders a forecast as a workbook with two sheets:
 * <ol>
 *   <li>the selected scenario: one row per line, 12 month columns (forecast months shaded), and full year, budget,
 *       variance and status. Full year, variance and every total are Excel formulas, so the workbook can be audited;</li>
 *   <li>a comparison of every scenario's full-year outlook against budget.</li>
 * </ol>
 */
@Component
public class ForecastWorkbookWriter {

    private static final int FIRST_MONTH_COLUMN = 1;
    private static final int FULL_YEAR_COLUMN = 13;
    private static final int BUDGET_COLUMN = 14;
    private static final int VARIANCE_COLUMN = 15;
    private static final int PERCENT_COLUMN = 16;
    private static final int STATUS_COLUMN = 17;

    public byte[] write(ForecastReport report, ScenarioComparison comparison) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            ExcelStyles styles = new ExcelStyles(workbook);
            writeForecastSheet(workbook.createSheet("Forecast - " + report.scenarioName()), report, styles);
            writeComparisonSheet(workbook.createSheet("Scenarios"), comparison, styles);
            return ExcelSheets.evaluateAndSave(workbook);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write forecast workbook", e);
        }
    }

    private static void writeForecastSheet(XSSFSheet sheet, ForecastReport report, ExcelStyles styles) {
        int rowIndex = ExcelSheets.writeHeading(sheet,
                "Full-year forecast – %s scenario".formatted(report.scenarioName()),
                List.of(periodText(report), "Forecast months are shaded. Full year = actuals + forecast."),
                styles);
        int headerRowIndex = rowIndex;
        writeHeader(sheet.createRow(rowIndex++), report.latestActualMonth(), styles);

        int firstDataRowIndex = rowIndex;
        List<Integer> outlineLevels = new ArrayList<>();
        List<Integer> revenueRows = new ArrayList<>();
        List<Integer> expenseRows = new ArrayList<>();
        for (ForecastDepartment department : report.departments()) {
            setText(sheet.createRow(rowIndex++).createCell(0), department.departmentName(), styles.bold.label());
            outlineLevels.add(0);
            for (ForecastLineRow line : department.lines()) {
                Row row = sheet.createRow(rowIndex++);
                writeLine(row, line, styles);
                outlineLevels.add(1);
                (line.accountType() == AccountType.REVENUE ? revenueRows : expenseRows).add(row.getRowNum() + 1);
            }
        }

        ForecastSummary summary = report.summary();
        int revenueRow = writeTotal(sheet.createRow(rowIndex++), "Total revenue", revenueRows, summary.revenue(), styles.bold, styles);
        int expenseRow = writeTotal(sheet.createRow(rowIndex++), "Total expenses", expenseRows, summary.expenses(), styles.bold, styles);
        writeNetIncome(sheet.createRow(rowIndex), revenueRow, expenseRow, summary.netIncome(), styles);
        outlineLevels.addAll(List.of(0, 0, 0));

        ExcelSheets.groupDetailRows(sheet, outlineLevels, firstDataRowIndex);
        ExcelSheets.layOut(sheet, headerRowIndex, 38, STATUS_COLUMN, 13);
    }

    private static void writeHeader(Row row, int latestActualMonth, ExcelStyles styles) {
        setText(row.createCell(0), "Department / account", styles.header);
        for (int month = 1; month <= 12; month++) {
            boolean actual = month <= latestActualMonth;
            setText(row.createCell(FIRST_MONTH_COLUMN + month - 1),
                    shortMonth(month) + (actual ? " (A)" : " (F)"),
                    actual ? styles.headerNumber : styles.headerForecast);
        }
        setText(row.createCell(FULL_YEAR_COLUMN), "Full year", styles.headerNumber);
        setText(row.createCell(BUDGET_COLUMN), "Budget", styles.headerNumber);
        setText(row.createCell(VARIANCE_COLUMN), "Variance", styles.headerNumber);
        setText(row.createCell(PERCENT_COLUMN), "Variance %", styles.headerNumber);
        setText(row.createCell(STATUS_COLUMN), "Status", styles.header);
    }

    private static void writeLine(Row row, ForecastLineRow line, ExcelStyles styles) {
        setText(row.createCell(0), line.accountCode() + " " + line.accountName(), styles.indentedDetail.label());
        for (MonthAmount month : line.months()) {
            setNumber(row.createCell(FIRST_MONTH_COLUMN + month.month() - 1), month.amount(),
                    month.actual() ? styles.indentedDetail.amount() : styles.forecastAmount);
        }
        writeOutlookColumns(row, line.outlook(), styles.indentedDetail, styles);
        setNumber(row.createCell(BUDGET_COLUMN), line.outlook().budget(), styles.indentedDetail.amount());
    }

    /** A total row whose month and budget cells are SUM formulas over the given rows. Returns its Excel row number. */
    private static int writeTotal(Row row, String label, List<Integer> sourceRows, OutlookLine outlook,
            RowStyles rowStyles, ExcelStyles styles) {
        setText(row.createCell(0), label, rowStyles.label());
        for (int column = FIRST_MONTH_COLUMN; column <= FIRST_MONTH_COLUMN + 11; column++) {
            setFormula(row.createCell(column), sumOf(column, sourceRows), rowStyles.amount());
        }
        setFormula(row.createCell(BUDGET_COLUMN), sumOf(BUDGET_COLUMN, sourceRows), rowStyles.amount());
        writeOutlookColumns(row, outlook, rowStyles, styles);
        return row.getRowNum() + 1;
    }

    private static void writeNetIncome(Row row, int revenueRow, int expenseRow, OutlookLine outlook, ExcelStyles styles) {
        RowStyles rowStyles = styles.total;
        setText(row.createCell(0), "Net income", rowStyles.label());
        for (int column = FIRST_MONTH_COLUMN; column <= FIRST_MONTH_COLUMN + 11; column++) {
            setFormula(row.createCell(column), difference(column, revenueRow, expenseRow), rowStyles.amount());
        }
        setFormula(row.createCell(BUDGET_COLUMN), difference(BUDGET_COLUMN, revenueRow, expenseRow), rowStyles.amount());
        writeOutlookColumns(row, outlook, rowStyles, styles);
    }

    /** Full year = SUM of the 12 months; variance and % are formulas against the budget column. */
    private static void writeOutlookColumns(Row row, OutlookLine outlook, RowStyles rowStyles, ExcelStyles styles) {
        int r = row.getRowNum() + 1;
        String fullYear = col(FULL_YEAR_COLUMN) + r;
        String budget = col(BUDGET_COLUMN) + r;
        String variance = col(VARIANCE_COLUMN) + r;
        setFormula(row.createCell(FULL_YEAR_COLUMN),
                "SUM(%s%d:%s%d)".formatted(col(FIRST_MONTH_COLUMN), r, col(FIRST_MONTH_COLUMN + 11), r), rowStyles.amount());
        setFormula(row.createCell(VARIANCE_COLUMN), "ROUND(%s-%s,2)".formatted(fullYear, budget), rowStyles.amount());
        setFormula(row.createCell(PERCENT_COLUMN),
                "IF(%s=0,\"n/a\",%s/ABS(%s))".formatted(budget, variance, budget), rowStyles.percent());
        setText(row.createCell(STATUS_COLUMN), ExcelStyles.statusText(outlook.favorability()),
                styles.status(outlook.favorability()));
    }

    private static void writeComparisonSheet(XSSFSheet sheet, ScenarioComparison comparison, ExcelStyles styles) {
        int rowIndex = ExcelSheets.writeHeading(sheet, "Scenario comparison – FY" + comparison.year(),
                List.of("Full-year outlook (actuals so far + forecast) for each scenario, against the full-year budget."),
                styles);
        int headerRowIndex = rowIndex;
        Row header = sheet.createRow(rowIndex++);
        setText(header.createCell(0), "Line", styles.header);
        setText(header.createCell(1), "Budget", styles.headerNumber);
        List<ScenarioOutlook> scenarios = comparison.scenarios();
        for (int i = 0; i < scenarios.size(); i++) {
            setText(header.createCell(2 + i), scenarios.get(i).scenarioName(), styles.headerNumber);
        }

        List<Function<ForecastSummary, OutlookLine>> lines = List.of(
                ForecastSummary::revenue, ForecastSummary::expenses, ForecastSummary::netIncome);
        List<String> labels = List.of("Revenue", "Expenses", "Net income");
        int netIncomeRow = 0;
        for (int l = 0; l < lines.size(); l++) {
            boolean isNet = l == lines.size() - 1;
            RowStyles rowStyles = isNet ? styles.total : styles.detail;
            Row row = sheet.createRow(rowIndex++);
            setText(row.createCell(0), labels.get(l), rowStyles.label());
            if (!scenarios.isEmpty()) {
                setNumber(row.createCell(1), lines.get(l).apply(scenarios.getFirst().summary()).budget(), rowStyles.amount());
            }
            for (int i = 0; i < scenarios.size(); i++) {
                setNumber(row.createCell(2 + i), lines.get(l).apply(scenarios.get(i).summary()).outlook(), rowStyles.amount());
            }
            netIncomeRow = row.getRowNum() + 1;
        }

        Row vsBudget = sheet.createRow(rowIndex);
        setText(vsBudget.createCell(0), "Net income vs. budget", styles.bold.label());
        for (int i = 0; i < scenarios.size(); i++) {
            String column = col(2 + i);
            setFormula(vsBudget.createCell(2 + i), "ROUND(%s%d-$B$%d,2)".formatted(column, netIncomeRow, netIncomeRow),
                    styles.bold.amount());
        }
        ExcelSheets.layOut(sheet, headerRowIndex, 28, 1 + scenarios.size(), 16);
    }

    private static String sumOf(int column, List<Integer> rows) {
        if (rows.isEmpty()) {
            return "0";
        }
        String letter = col(column);
        return "SUM(" + rows.stream().map(r -> letter + r).collect(Collectors.joining(",")) + ")";
    }

    private static String difference(int column, int minuendRow, int subtrahendRow) {
        String letter = col(column);
        return "%s%d-%s%d".formatted(letter, minuendRow, letter, subtrahendRow);
    }

    private static String col(int columnIndex) {
        return CellReference.convertNumToColString(columnIndex);
    }

    private static String periodText(ForecastReport report) {
        if (report.latestActualMonth() == 0) {
            return "FY%d: no actuals yet, all 12 months are forecast.".formatted(report.year());
        }
        if (report.latestActualMonth() == 12) {
            return "FY%d: all 12 months are actuals.".formatted(report.year());
        }
        return "FY%d: actuals January to %s, forecast %s to December.".formatted(report.year(),
                longMonth(report.latestActualMonth()), longMonth(report.latestActualMonth() + 1));
    }

    private static String shortMonth(int month) {
        return Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private static String longMonth(int month) {
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }
}
