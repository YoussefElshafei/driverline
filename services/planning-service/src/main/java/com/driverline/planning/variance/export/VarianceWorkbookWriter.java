package com.driverline.planning.variance.export;

import static com.driverline.planning.common.excel.ExcelSheets.setFormula;
import static com.driverline.planning.common.excel.ExcelSheets.setNumber;
import static com.driverline.planning.common.excel.ExcelSheets.setText;

import com.driverline.planning.common.excel.ExcelSheets;
import com.driverline.planning.common.excel.ExcelStyles;
import com.driverline.planning.common.excel.ExcelStyles.RowStyles;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/**
 * Renders an {@link ExcelReport} as a formatted .xlsx workbook.
 *
 * <p>Budget and actual are values; variance and variance % are live Excel formulas, so a finance user can audit
 * them and they recalculate if someone edits a number. Detail rows are grouped so Excel shows +/- buttons
 * to expand or collapse each department.
 */
@Component
public class VarianceWorkbookWriter {

    static final List<String> NUMBER_HEADERS = List.of("Budget", "Actual", "Variance", "Variance %", "Status");

    private static final int LABEL_COLUMN_WIDTH = 46;
    private static final int NUMBER_COLUMN_WIDTH = 16;

    public byte[] write(ExcelReport report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            ExcelStyles styles = new ExcelStyles(workbook);
            XSSFSheet sheet = workbook.createSheet(report.sheetName());

            int rowIndex = ExcelSheets.writeHeading(sheet, report.title(), report.subtitles(), styles);
            int headerRowIndex = rowIndex;
            writeColumnHeaders(sheet.createRow(rowIndex++), report.labelHeader(), styles);
            int firstDataRowIndex = rowIndex;
            for (ExcelRow row : report.rows()) {
                writeRow(sheet.createRow(rowIndex++), row, styles);
            }

            ExcelSheets.groupDetailRows(sheet, report.rows().stream().map(ExcelRow::outlineLevel).toList(), firstDataRowIndex);
            ExcelSheets.layOut(sheet, headerRowIndex, LABEL_COLUMN_WIDTH, NUMBER_HEADERS.size(), NUMBER_COLUMN_WIDTH);
            return ExcelSheets.evaluateAndSave(workbook);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write Excel report", e);
        }
    }

    private static void writeColumnHeaders(Row row, String labelHeader, ExcelStyles styles) {
        setText(row.createCell(0), labelHeader, styles.header);
        for (int i = 0; i < NUMBER_HEADERS.size(); i++) {
            setText(row.createCell(i + 1), NUMBER_HEADERS.get(i), styles.headerNumber);
        }
    }

    private static void writeRow(Row row, ExcelRow data, ExcelStyles styles) {
        RowStyles rowStyles = stylesFor(data, styles);
        setText(row.createCell(0), data.label(), rowStyles.label());
        if (!data.hasNumbers()) {
            return;
        }
        int excelRow = row.getRowNum() + 1; // formulas use Excel's 1-based row numbers
        setNumber(row.createCell(1), data.budget(), rowStyles.amount());
        if (data.actual() == null) {
            setText(row.createCell(5), "Not closed", styles.statusNotClosed);
            return;
        }
        setNumber(row.createCell(2), data.actual(), rowStyles.amount());
        // ROUND keeps the stored value at whole cents; Excel subtracts in binary floating point, which can leave tails
        // like -1251.2700000000186 that would leak into any formula referencing this cell.
        setFormula(row.createCell(3), "ROUND(C%d-B%d,2)".formatted(excelRow, excelRow), rowStyles.amount());
        setFormula(row.createCell(4), "IF(B%d=0,\"n/a\",D%d/ABS(B%d))".formatted(excelRow, excelRow, excelRow),
                rowStyles.percent());
        setText(row.createCell(5), ExcelStyles.statusText(data.favorability()), styles.status(data.favorability()));
    }

    private static RowStyles stylesFor(ExcelRow data, ExcelStyles styles) {
        return switch (data.kind()) {
            case SECTION, SUBTOTAL -> styles.bold;
            case DETAIL -> data.outlineLevel() > 0 ? styles.indentedDetail : styles.detail;
            case TOTAL -> styles.total;
        };
    }
}
