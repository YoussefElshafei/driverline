package com.driverline.planning.common.error;

import com.driverline.planning.actual.importing.ImportValidationException;
import com.driverline.planning.actual.importing.UnsupportedFileTypeException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Turns every error into an RFC 9457 "problem details" response, so clients always get the same JSON shape:
 * {@code type, title, status, detail, instance} plus an {@code errors} list when specific fields or rows are at fault.
 * Extending {@link ResponseEntityExceptionHandler} gives the same shape to Spring's own errors
 * (malformed JSON, missing parameters, wrong HTTP method, upload too large, ...).
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERRORS_PROPERTY = "errors";

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
        ProblemDetail problem = validationProblem("One or more fields are invalid.", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorDetail> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorDetail(
                                result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        ProblemDetail problem = validationProblem("One or more request parameters are invalid.", errors);
        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(RequestValidationException.class)
    ProblemDetail handleRequestValidation(RequestValidationException ex) {
        return validationProblem(ex.getMessage(), ex.getErrors());
    }

    @ExceptionHandler(ImportValidationException.class)
    ProblemDetail handleImportValidation(ImportValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problem.setTitle("Import failed");
        problem.setProperty(ERRORS_PROPERTY, ex.getErrors());
        return problem;
    }

    @ExceptionHandler(UnsupportedFileTypeException.class)
    ProblemDetail handleUnsupportedFileType(UnsupportedFileTypeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.getMessage());
        problem.setTitle("Unsupported file type");
        return problem;
    }

    /** Last resort: log the real cause for us, but never leak stack traces or internals to the client. */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong on our side. Please try again.");
        problem.setTitle("Unexpected error");
        return problem;
    }

    private static ProblemDetail validationProblem(String detail, List<FieldErrorDetail> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation failed");
        problem.setProperty(ERRORS_PROPERTY, errors);
        return problem;
    }
}
