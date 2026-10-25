package com.driverline.planning.actual.importing;

import com.driverline.planning.common.LineKey;
import com.driverline.planning.common.money.MoneyParser;
import com.driverline.planning.common.money.MoneyParser.InvalidMoneyException;
import com.driverline.planning.period.FiscalPeriod;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Checks every raw row and collects all problems instead of stopping at the first,
 * so a user can fix the whole file in one pass.
 */
@Component
public class ActualRowValidator {

    public record Result(List<ValidActualRow> validRows, List<RowError> errors) {

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }

    public Result validate(List<RawActualRow> rows, Set<String> knownAccountCodes, Set<String> knownDepartmentCodes) {
        List<ValidActualRow> validRows = new ArrayList<>();
        List<RowError> errors = new ArrayList<>();
        Map<LineKey, Integer> firstRowByKey = new HashMap<>();

        for (RawActualRow raw : rows) {
            RowChecker checker = new RowChecker(raw.rowNumber(), errors);

            Integer year = checker.wholeNumber(ActualsFileFormat.YEAR, raw.year(), FiscalPeriod.MIN_YEAR, FiscalPeriod.MAX_YEAR);
            Integer month = checker.wholeNumber(ActualsFileFormat.MONTH, raw.month(), 1, 12);
            String accountCode = checker.knownCode(ActualsFileFormat.ACCOUNT_CODE, raw.accountCode(), knownAccountCodes, "account");
            String departmentCode = checker.knownCode(
                    ActualsFileFormat.DEPARTMENT_CODE, raw.departmentCode().toUpperCase(Locale.ROOT), knownDepartmentCodes, "department");
            BigDecimal amount = checker.money(ActualsFileFormat.AMOUNT, raw.amount());

            if (checker.isValid()) {
                var row = new ValidActualRow(raw.rowNumber(), accountCode, departmentCode, FiscalPeriod.of(year, month), amount);
                Integer duplicateOf = firstRowByKey.putIfAbsent(row.key(), raw.rowNumber());
                if (duplicateOf != null) {
                    errors.add(new RowError(raw.rowNumber(), null, null,
                            "Duplicate of row %d (same year, month, account and department)".formatted(duplicateOf)));
                } else {
                    validRows.add(row);
                }
            }
        }
        return new Result(validRows, errors);
    }

    /** Validates the fields of a single row, recording each problem. Returns null for a field that failed. */
    private static final class RowChecker {

        private final int rowNumber;
        private final List<RowError> errors;
        private boolean valid = true;

        RowChecker(int rowNumber, List<RowError> errors) {
            this.rowNumber = rowNumber;
            this.errors = errors;
        }

        boolean isValid() {
            return valid;
        }

        Integer wholeNumber(String column, String text, int min, int max) {
            if (isMissing(column, text)) {
                return null;
            }
            try {
                int value = Integer.parseInt(text);
                if (value < min || value > max) {
                    return fail(column, text, "must be between %d and %d".formatted(min, max));
                }
                return value;
            } catch (NumberFormatException e) {
                return fail(column, text, "must be a whole number");
            }
        }

        String knownCode(String column, String code, Set<String> knownCodes, String kind) {
            if (isMissing(column, code)) {
                return null;
            }
            if (!knownCodes.contains(code)) {
                return fail(column, code, "Unknown %s code '%s'".formatted(kind, code));
            }
            return code;
        }

        BigDecimal money(String column, String text) {
            if (isMissing(column, text)) {
                return null;
            }
            try {
                return MoneyParser.parse(text);
            } catch (InvalidMoneyException e) {
                return fail(column, text, "'%s' %s".formatted(text, e.getMessage()));
            }
        }

        private boolean isMissing(String column, String text) {
            if (text == null || text.isBlank()) {
                fail(column, "", "is required");
                return true;
            }
            return false;
        }

        private <T> T fail(String column, String value, String message) {
            errors.add(new RowError(rowNumber, column, value, message));
            valid = false;
            return null;
        }
    }
}
