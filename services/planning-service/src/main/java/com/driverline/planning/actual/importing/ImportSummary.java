package com.driverline.planning.actual.importing;

import java.util.List;

/**
 * @param created actual lines that didn't exist before
 * @param updated existing actual lines whose amount was replaced by the file's value
 * @param periods the months the file touched, as "YYYY-MM", in order
 */
public record ImportSummary(String fileName, int rowsImported, int created, int updated, List<String> periods) {
}
