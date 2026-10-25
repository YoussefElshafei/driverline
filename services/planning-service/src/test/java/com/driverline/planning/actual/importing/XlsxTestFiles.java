package com.driverline.planning.actual.importing;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/** Builds small .xlsx files in memory for tests. Numbers become numeric cells, strings become text cells. */
public final class XlsxTestFiles {

    /** Wrap a value in this to write it as an Excel formula, e.g. {@code new Formula("100+23.45")}. */
    public record Formula(String expression) {
    }

    private XlsxTestFiles() {
    }

    public static byte[] workbook(List<List<Object>> rows) {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Actuals");
            for (int r = 0; r < rows.size(); r++) {
                List<Object> values = rows.get(r);
                if (values == null) {
                    continue; // leave the row physically missing
                }
                Row row = sheet.createRow(r);
                for (int c = 0; c < values.size(); c++) {
                    Object value = values.get(c);
                    switch (value) {
                        case null -> { }
                        case Number number -> row.createCell(c).setCellValue(number.doubleValue());
                        case Formula formula -> row.createCell(c).setCellFormula(formula.expression());
                        default -> row.createCell(c).setCellValue(value.toString());
                    }
                }
            }
            // Store calculated formula results in the file, as Excel does when it saves.
            workbook.getCreationHelper().createFormulaEvaluator().evaluateAll();
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
