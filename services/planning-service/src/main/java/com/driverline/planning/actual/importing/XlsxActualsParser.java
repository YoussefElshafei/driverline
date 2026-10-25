package com.driverline.planning.actual.importing;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.NumberToTextConverter;
import org.springframework.stereotype.Component;

/** Reads the first worksheet of an .xlsx workbook. Row 1 must be the column headers. */
@Component
public class XlsxActualsParser implements ActualsFileParser {

    private final DataFormatter dataFormatter = new DataFormatter(Locale.ROOT);

    @Override
    public boolean supports(String fileName) {
        return fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx");
    }

    @Override
    public List<RawActualRow> parse(InputStream content) {
        try (Workbook workbook = WorkbookFactory.create(content)) {
            if (workbook.getNumberOfSheets() == 0) {
                throw ImportValidationException.forFile("The workbook has no worksheets.");
            }
            return readSheet(workbook.getSheetAt(0));
        } catch (ImportValidationException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            // POI throws various unchecked exceptions for files that aren't real .xlsx workbooks (renamed files, corrupt zips).
            throw ImportValidationException.forFile("The file could not be read as an .xlsx workbook.");
        }
    }

    private List<RawActualRow> readSheet(Sheet sheet) {
        Row headerRow = sheet.getRow(sheet.getFirstRowNum());
        if (sheet.getPhysicalNumberOfRows() == 0 || headerRow == null) {
            throw ImportValidationException.forFile("The first worksheet is empty. Row 1 must be the column headers.");
        }
        Map<String, Integer> columnIndexes = columnIndexes(headerRow);
        ActualsFileFormat.requireColumns(columnIndexes.keySet());

        List<RawActualRow> rows = new ArrayList<>();
        for (int r = headerRow.getRowNum() + 1; r <= sheet.getLastRowNum(); r++) {
            Row row = sheet.getRow(r);
            if (row == null || isBlank(row)) {
                continue;
            }
            rows.add(new RawActualRow(
                    r + 1, // POI rows start at 0; Excel shows them starting at 1.
                    text(row, columnIndexes.get(ActualsFileFormat.YEAR)),
                    text(row, columnIndexes.get(ActualsFileFormat.MONTH)),
                    text(row, columnIndexes.get(ActualsFileFormat.ACCOUNT_CODE)),
                    text(row, columnIndexes.get(ActualsFileFormat.DEPARTMENT_CODE)),
                    text(row, columnIndexes.get(ActualsFileFormat.AMOUNT))));
            ActualsFileFormat.requireWithinRowLimit(rows.size());
        }
        return rows;
    }

    private Map<String, Integer> columnIndexes(Row headerRow) {
        Map<String, Integer> indexes = new HashMap<>();
        for (Cell cell : headerRow) {
            indexes.putIfAbsent(ActualsFileFormat.normalizeHeader(cellText(cell, cell.getCellType())), cell.getColumnIndex());
        }
        return indexes;
    }

    private String text(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        return cell == null ? "" : cellText(cell, cell.getCellType()).trim();
    }

    private boolean isBlank(Row row) {
        for (Cell cell : row) {
            if (!cellText(cell, cell.getCellType()).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private String cellText(Cell cell, CellType type) {
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? dataFormatter.formatCellValue(cell)
                    // Excel stores numbers as binary doubles. NumberToTextConverter renders them the way Excel displays
                    // them (15 significant digits), so 1234.56 becomes "1234.56", not "1234.5599999999999".
                    : NumberToTextConverter.toText(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            // For formulas, read the result Excel last calculated and saved in the file.
            case FORMULA -> cellText(cell, cell.getCachedFormulaResultType());
            case ERROR -> "#ERROR";
            case BLANK, _NONE -> "";
        };
    }
}
