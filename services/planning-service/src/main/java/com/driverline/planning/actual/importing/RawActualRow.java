package com.driverline.planning.actual.importing;

/**
 * One data row exactly as read from a file: every value is still text (blank when missing).
 * Both the CSV and the XLSX parser produce these, so a single validator handles both formats.
 */
public record RawActualRow(
        int rowNumber, String year, String month, String accountCode, String departmentCode, String amount) {
}
