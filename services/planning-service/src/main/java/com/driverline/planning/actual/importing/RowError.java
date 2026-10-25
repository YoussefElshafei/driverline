package com.driverline.planning.actual.importing;

/**
 * One problem in an imported file.
 *
 * @param row    the row number as the user sees it in Excel or a text editor (the header is row 1);
 *               {@code null} for problems with the whole file, such as a missing column
 * @param column the column at fault, or {@code null} if the problem isn't tied to one column
 * @param value  the offending value as it appeared in the file, or {@code null}
 */
public record RowError(Integer row, String column, String value, String message) {

    public static RowError forFile(String message) {
        return new RowError(null, null, null, message);
    }
}
