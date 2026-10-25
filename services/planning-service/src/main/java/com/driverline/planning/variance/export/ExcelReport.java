package com.driverline.planning.variance.export;

import java.util.List;

/**
 * A variance report laid out for a spreadsheet, independent of POI. The writer renders any ExcelReport,
 * so each report type only needs a small mapping, and the layout can be tested without opening a workbook.
 *
 * @param labelHeader heading of the first column, e.g. "Department / account"
 */
public record ExcelReport(
        String fileName, String sheetName, String title, List<String> subtitles, String labelHeader, List<ExcelRow> rows) {
}
