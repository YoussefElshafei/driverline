package com.driverline.planning.variance.export;

import com.driverline.planning.variance.Favorability;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
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

    private static final String AMOUNT_FORMAT = "#,##0.00;(#,##0.00)";
    private static final String PERCENT_FORMAT = "0.0%;(0.0%)";
    private static final int LABEL_COLUMN_WIDTH = 46;
    private static final int NUMBER_COLUMN_WIDTH = 16;

    public byte[] write(ExcelReport report) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Styles styles = new Styles(workbook);
            XSSFSheet sheet = workbook.createSheet(report.sheetName());

            int rowIndex = writeHeading(sheet, report, styles);
            int headerRowIndex = rowIndex;
            writeColumnHeaders(sheet.createRow(rowIndex++), report.labelHeader(), styles);
            int firstDataRowIndex = rowIndex;
            for (ExcelRow row : report.rows()) {
                writeRow(sheet.createRow(rowIndex++), row, styles);
            }

            groupDetailRows(sheet, report.rows(), firstDataRowIndex);
            layOut(sheet, headerRowIndex);
            // Store calculated formula results in the file, so viewers that don't recalculate still show numbers.
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write Excel report", e);
        }
    }

    private static int writeHeading(XSSFSheet sheet, ExcelReport report, Styles styles) {
        int rowIndex = 0;
        setText(sheet.createRow(rowIndex++).createCell(0), report.title(), styles.title);
        for (String subtitle : report.subtitles()) {
            setText(sheet.createRow(rowIndex++).createCell(0), subtitle, styles.subtitle);
        }
        return rowIndex + 1; // one blank row before the table
    }

    private static void writeColumnHeaders(Row row, String labelHeader, Styles styles) {
        setText(row.createCell(0), labelHeader, styles.header);
        for (int i = 0; i < NUMBER_HEADERS.size(); i++) {
            setText(row.createCell(i + 1), NUMBER_HEADERS.get(i), styles.headerNumber);
        }
    }

    private static void writeRow(Row row, ExcelRow data, Styles styles) {
        RowStyles rowStyles = styles.forKind(data.kind(), data.outlineLevel());
        setText(row.createCell(0), data.label(), rowStyles.label());
        if (!data.hasNumbers()) {
            return;
        }
        int excelRow = row.getRowNum() + 1; // formulas use Excel's 1-based row numbers
        setNumber(row.createCell(1), data, true, rowStyles.amount());
        if (data.actual() == null) {
            setText(row.createCell(5), "Not closed", styles.statusNotClosed);
            return;
        }
        setNumber(row.createCell(2), data, false, rowStyles.amount());
        // ROUND keeps the stored value at whole cents; Excel subtracts in binary floating point, which can leave tails
        // like -1251.2700000000186 that would leak into any formula referencing this cell.
        setFormula(row.createCell(3), "ROUND(C%d-B%d,2)".formatted(excelRow, excelRow), rowStyles.amount());
        setFormula(row.createCell(4), "IF(B%d=0,\"n/a\",D%d/ABS(B%d))".formatted(excelRow, excelRow, excelRow),
                rowStyles.percent());
        setText(row.createCell(5), statusText(data.favorability()), styles.status(data.favorability()));
    }

    /** Groups each run of consecutive outline-level-1 rows under the row above it (the department). */
    private static void groupDetailRows(XSSFSheet sheet, List<ExcelRow> rows, int firstDataRowIndex) {
        sheet.setRowSumsBelow(false); // the +/- button sits on the department row above its accounts
        int runStart = -1;
        for (int i = 0; i <= rows.size(); i++) {
            boolean detail = i < rows.size() && rows.get(i).outlineLevel() == 1;
            if (detail && runStart < 0) {
                runStart = i;
            } else if (!detail && runStart >= 0) {
                sheet.groupRow(firstDataRowIndex + runStart, firstDataRowIndex + i - 1);
                runStart = -1;
            }
        }
    }

    private static void layOut(XSSFSheet sheet, int headerRowIndex) {
        sheet.createFreezePane(1, headerRowIndex + 1);
        sheet.setColumnWidth(0, LABEL_COLUMN_WIDTH * 256);
        for (int column = 1; column <= NUMBER_HEADERS.size(); column++) {
            sheet.setColumnWidth(column, NUMBER_COLUMN_WIDTH * 256);
        }
        sheet.getPrintSetup().setLandscape(true);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    private static String statusText(Favorability favorability) {
        return switch (favorability) {
            case FAVORABLE -> "Favorable";
            case UNFAVORABLE -> "Unfavorable";
            case ON_BUDGET -> "On budget";
        };
    }

    private static void setText(Cell cell, String text, CellStyle style) {
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    /** Excel itself stores numbers as doubles; amounts with 2 decimals are well within its 15 exact digits. */
    private static void setNumber(Cell cell, ExcelRow data, boolean budget, CellStyle style) {
        cell.setCellValue((budget ? data.budget() : data.actual()).doubleValue());
        cell.setCellStyle(style);
    }

    private static void setFormula(Cell cell, String formula, CellStyle style) {
        cell.setCellFormula(formula);
        cell.setCellStyle(style);
    }

    private record RowStyles(CellStyle label, CellStyle amount, CellStyle percent) {
    }

    /** POI workbooks have a limit on distinct styles, so each style is created once and reused. */
    private static final class Styles {

        final CellStyle title;
        final CellStyle subtitle;
        final CellStyle header;
        final CellStyle headerNumber;
        final CellStyle statusFavorable;
        final CellStyle statusUnfavorable;
        final CellStyle statusOnBudget;
        final CellStyle statusNotClosed;
        final RowStyles detail;
        final RowStyles indentedDetail;
        final RowStyles section;
        final RowStyles subtotal;
        final RowStyles total;

        Styles(XSSFWorkbook workbook) {
            short amountFormat = workbook.createDataFormat().getFormat(AMOUNT_FORMAT);
            short percentFormat = workbook.createDataFormat().getFormat(PERCENT_FORMAT);
            Font bold = font(workbook, true, (short) 11);

            title = workbook.createCellStyle();
            title.setFont(font(workbook, true, (short) 14));
            subtitle = workbook.createCellStyle();
            Font grey = font(workbook, false, (short) 10);
            grey.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            subtitle.setFont(grey);

            header = filled(workbook, IndexedColors.GREY_25_PERCENT);
            header.setFont(bold);
            header.setBorderBottom(BorderStyle.THIN);
            headerNumber = workbook.createCellStyle();
            headerNumber.cloneStyleFrom(header);
            headerNumber.setAlignment(HorizontalAlignment.RIGHT);

            statusFavorable = filled(workbook, IndexedColors.LIGHT_GREEN);
            statusUnfavorable = filled(workbook, IndexedColors.ROSE);
            statusOnBudget = workbook.createCellStyle();
            statusNotClosed = workbook.createCellStyle();
            statusNotClosed.setFont(grey);

            detail = rowStyles(workbook, null, false, 0, amountFormat, percentFormat);
            indentedDetail = rowStyles(workbook, null, false, 2, amountFormat, percentFormat);
            section = rowStyles(workbook, bold, false, 0, amountFormat, percentFormat);
            subtotal = rowStyles(workbook, bold, false, 0, amountFormat, percentFormat);
            total = rowStyles(workbook, bold, true, 0, amountFormat, percentFormat);
        }

        RowStyles forKind(ExcelRow.Kind kind, int outlineLevel) {
            return switch (kind) {
                case SECTION -> section;
                case DETAIL -> outlineLevel > 0 ? indentedDetail : detail;
                case SUBTOTAL -> subtotal;
                case TOTAL -> total;
            };
        }

        CellStyle status(Favorability favorability) {
            return switch (favorability) {
                case FAVORABLE -> statusFavorable;
                case UNFAVORABLE -> statusUnfavorable;
                case ON_BUDGET -> statusOnBudget;
            };
        }

        private static RowStyles rowStyles(XSSFWorkbook workbook, Font font, boolean topBorder, int indent,
                short amountFormat, short percentFormat) {
            CellStyle label = base(workbook, font, topBorder);
            label.setIndention((short) indent);
            CellStyle amount = base(workbook, font, topBorder);
            amount.setDataFormat(amountFormat);
            CellStyle percent = base(workbook, font, topBorder);
            percent.setDataFormat(percentFormat);
            percent.setAlignment(HorizontalAlignment.RIGHT);
            return new RowStyles(label, amount, percent);
        }

        private static CellStyle base(XSSFWorkbook workbook, Font font, boolean topBorder) {
            CellStyle style = workbook.createCellStyle();
            if (font != null) {
                style.setFont(font);
            }
            if (topBorder) {
                style.setBorderTop(BorderStyle.THIN);
            }
            return style;
        }

        private static CellStyle filled(XSSFWorkbook workbook, IndexedColors color) {
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(color.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            return style;
        }

        private static Font font(XSSFWorkbook workbook, boolean bold, short points) {
            Font font = workbook.createFont();
            font.setBold(bold);
            font.setFontHeightInPoints(points);
            return font;
        }
    }
}
