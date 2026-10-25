package com.driverline.planning.common.excel;

import com.driverline.planning.variance.Favorability;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The shared look of every exported workbook. A workbook has a limited number of distinct styles,
 * so each style is created once per workbook and reused for every cell.
 */
public final class ExcelStyles {

    public static final String AMOUNT_FORMAT = "#,##0.00;(#,##0.00)";
    public static final String PERCENT_FORMAT = "0.0%;(0.0%)";
    public static final String QUANTITY_FORMAT = "#,##0.##";

    /** Label, amount and percent styles for one kind of row. */
    public record RowStyles(CellStyle label, CellStyle amount, CellStyle percent) {
    }

    public final CellStyle title;
    public final CellStyle subtitle;
    public final CellStyle header;
    public final CellStyle headerNumber;
    /** Header for forecast columns, shaded to match the forecast cells below it. */
    public final CellStyle headerForecast;
    public final CellStyle statusFavorable;
    public final CellStyle statusUnfavorable;
    public final CellStyle statusOnBudget;
    public final CellStyle statusNotClosed;
    public final RowStyles detail;
    public final RowStyles indentedDetail;
    public final RowStyles bold;
    public final RowStyles total;
    /** Amount style for forecast (not actual) months: shaded so the reader can tell them apart. */
    public final CellStyle forecastAmount;

    public ExcelStyles(XSSFWorkbook workbook) {
        short amountFormat = workbook.createDataFormat().getFormat(AMOUNT_FORMAT);
        short percentFormat = workbook.createDataFormat().getFormat(PERCENT_FORMAT);
        Font boldFont = font(workbook, true, (short) 11);
        Font grey = font(workbook, false, (short) 10);
        grey.setColor(IndexedColors.GREY_50_PERCENT.getIndex());

        title = workbook.createCellStyle();
        title.setFont(font(workbook, true, (short) 14));
        subtitle = workbook.createCellStyle();
        subtitle.setFont(grey);

        header = filled(workbook, IndexedColors.GREY_25_PERCENT);
        header.setFont(boldFont);
        header.setBorderBottom(BorderStyle.THIN);
        headerNumber = workbook.createCellStyle();
        headerNumber.cloneStyleFrom(header);
        headerNumber.setAlignment(HorizontalAlignment.RIGHT);
        headerForecast = workbook.createCellStyle();
        headerForecast.cloneStyleFrom(headerNumber);
        headerForecast.setFillForegroundColor(IndexedColors.PALE_BLUE.getIndex());

        statusFavorable = filled(workbook, IndexedColors.LIGHT_GREEN);
        statusUnfavorable = filled(workbook, IndexedColors.ROSE);
        statusOnBudget = workbook.createCellStyle();
        statusNotClosed = workbook.createCellStyle();
        statusNotClosed.setFont(grey);

        detail = rowStyles(workbook, null, false, 0, amountFormat, percentFormat);
        indentedDetail = rowStyles(workbook, null, false, 2, amountFormat, percentFormat);
        bold = rowStyles(workbook, boldFont, false, 0, amountFormat, percentFormat);
        total = rowStyles(workbook, boldFont, true, 0, amountFormat, percentFormat);

        forecastAmount = filled(workbook, IndexedColors.PALE_BLUE);
        forecastAmount.setDataFormat(amountFormat);
    }

    public CellStyle status(Favorability favorability) {
        return switch (favorability) {
            case FAVORABLE -> statusFavorable;
            case UNFAVORABLE -> statusUnfavorable;
            case ON_BUDGET -> statusOnBudget;
        };
    }

    public static String statusText(Favorability favorability) {
        return switch (favorability) {
            case FAVORABLE -> "Favorable";
            case UNFAVORABLE -> "Unfavorable";
            case ON_BUDGET -> "On budget";
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
