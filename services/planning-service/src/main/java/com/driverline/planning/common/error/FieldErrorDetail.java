package com.driverline.planning.common.error;

/** One invalid field in a request, e.g. {@code entries[2].amount: must have at most 2 decimal places}. */
public record FieldErrorDetail(String field, String message) {
}
