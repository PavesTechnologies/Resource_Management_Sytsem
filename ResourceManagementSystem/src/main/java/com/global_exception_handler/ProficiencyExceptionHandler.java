package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class ProficiencyExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public ProficiencyExceptionHandler(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static ProficiencyExceptionHandler notFound(String message) {
        return new ProficiencyExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ProficiencyExceptionHandler badRequest(String message) {
        return new ProficiencyExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static ProficiencyExceptionHandler conflict(String message) {
        return new ProficiencyExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
