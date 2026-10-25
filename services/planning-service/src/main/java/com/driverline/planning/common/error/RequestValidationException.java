package com.driverline.planning.common.error;

import java.util.List;

/**
 * A request that is well-formed but breaks a business rule that annotations can't express,
 * such as referring to an account code that doesn't exist. Returned as HTTP 400.
 */
public class RequestValidationException extends RuntimeException {

    private final transient List<FieldErrorDetail> errors;

    public RequestValidationException(String message, List<FieldErrorDetail> errors) {
        super(message);
        this.errors = List.copyOf(errors);
    }

    public static RequestValidationException forField(String field, String message) {
        return new RequestValidationException(message, List.of(new FieldErrorDetail(field, message)));
    }

    public List<FieldErrorDetail> getErrors() {
        return errors;
    }
}
