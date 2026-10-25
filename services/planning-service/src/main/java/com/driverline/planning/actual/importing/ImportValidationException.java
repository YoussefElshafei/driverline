package com.driverline.planning.actual.importing;

import java.util.List;

/** The file was rejected and nothing was imported. Returned as HTTP 422 with every problem listed. */
public class ImportValidationException extends RuntimeException {

    private final transient List<RowError> errors;

    public ImportValidationException(List<RowError> errors) {
        super(summary(errors.size()));
        this.errors = List.copyOf(errors);
    }

    public static ImportValidationException forFile(String message) {
        return new ImportValidationException(List.of(RowError.forFile(message)));
    }

    public List<RowError> getErrors() {
        return errors;
    }

    private static String summary(int errorCount) {
        String problems = errorCount == 1 ? "1 problem" : errorCount + " problems";
        return "Found " + problems + " in the file. No rows were imported.";
    }
}
