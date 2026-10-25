package com.driverline.planning.actual.importing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** The columns an actuals file must contain, in any order. Header names are matched case-insensitively. */
public final class ActualsFileFormat {

    public static final String YEAR = "year";
    public static final String MONTH = "month";
    public static final String ACCOUNT_CODE = "account_code";
    public static final String DEPARTMENT_CODE = "department_code";
    public static final String AMOUNT = "amount";

    public static final List<String> REQUIRED_COLUMNS = List.of(YEAR, MONTH, ACCOUNT_CODE, DEPARTMENT_CODE, AMOUNT);

    /** Guards against accidentally uploading something huge; a small company's monthly actuals are far smaller. */
    public static final int MAX_DATA_ROWS = 10_000;

    public static final String TEMPLATE_CSV = String.join(",", REQUIRED_COLUMNS) + "\n"
            + "2026,10,4000,SALES,301250.00\n"
            + "2026,10,5300,FLEET,17890.45\n";

    private ActualsFileFormat() {
    }

    /** @throws ImportValidationException listing every required column the header is missing */
    public static void requireColumns(Collection<String> normalizedHeaders) {
        List<RowError> missing = new ArrayList<>();
        for (String column : REQUIRED_COLUMNS) {
            if (!normalizedHeaders.contains(column)) {
                missing.add(new RowError(1, column, null, "Missing required column '%s'".formatted(column)));
            }
        }
        if (!missing.isEmpty()) {
            throw new ImportValidationException(missing);
        }
    }

    public static void requireWithinRowLimit(int dataRowCount) {
        if (dataRowCount > MAX_DATA_ROWS) {
            throw ImportValidationException.forFile(
                    "The file has more than %,d data rows. Split it into smaller files.".formatted(MAX_DATA_ROWS));
        }
    }

    public static String normalizeHeader(String header) {
        return header == null ? "" : header.trim().toLowerCase();
    }
}
