package com.driverline.planning.common.excel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Small helpers shared by the Excel writers: headings, grouping, layout and saving. */
public final class ExcelSheets {

    private ExcelSheets() {
    }

    /** Writes the title and subtitle rows, leaves one blank row, and returns the index of the next free row. */
    public static int writeHeading(XSSFSheet sheet, String title, List<String> subtitles, ExcelStyles styles) {
        int rowIndex = 0;
        setText(sheet.createRow(rowIndex++).createCell(0), title, styles.title);
        for (String subtitle : subtitles) {
            setText(sheet.createRow(rowIndex++).createCell(0), subtitle, styles.subtitle);
        }
        return rowIndex + 1;
    }

    /**
     * Groups each run of consecutive outline-level-1 rows under the row above it, so Excel shows a +/- button
     * on that row to expand or collapse them.
     *
     * @param outlineLevels outline level of each data row, in order
     */
    public static void groupDetailRows(XSSFSheet sheet, List<Integer> outlineLevels, int firstDataRowIndex) {
        sheet.setRowSumsBelow(false);
        int runStart = -1;
        for (int i = 0; i <= outlineLevels.size(); i++) {
            boolean detail = i < outlineLevels.size() && outlineLevels.get(i) == 1;
            if (detail && runStart < 0) {
                runStart = i;
            } else if (!detail && runStart >= 0) {
                sheet.groupRow(firstDataRowIndex + runStart, firstDataRowIndex + i - 1);
                runStart = -1;
            }
        }
    }

    /** Freezes the label column and everything above the first data row, sets widths, and fits printing to one page wide. */
    public static void layOut(XSSFSheet sheet, int headerRowIndex, int labelWidthChars, int numberColumns,
            int numberWidthChars) {
        sheet.createFreezePane(1, headerRowIndex + 1);
        sheet.setColumnWidth(0, labelWidthChars * 256);
        for (int column = 1; column <= numberColumns; column++) {
            sheet.setColumnWidth(column, numberWidthChars * 256);
        }
        sheet.getPrintSetup().setLandscape(true);
        sheet.setFitToPage(true);
        sheet.getPrintSetup().setFitWidth((short) 1);
        sheet.getPrintSetup().setFitHeight((short) 0);
    }

    /** Calculates and stores every formula result (so viewers that don't recalculate still show numbers), then saves. */
    public static byte[] evaluateAndSave(XSSFWorkbook workbook) {
        workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write Excel workbook", e);
        }
    }

    public static void setText(Cell cell, String text, CellStyle style) {
        cell.setCellValue(text);
        cell.setCellStyle(style);
    }

    /** Excel stores numbers as doubles; amounts with 2 decimals are well within its 15 exact digits. */
    public static void setNumber(Cell cell, BigDecimal value, CellStyle style) {
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(style);
    }

    public static void setFormula(Cell cell, String formula, CellStyle style) {
        cell.setCellFormula(formula);
        cell.setCellStyle(style);
    }
}
