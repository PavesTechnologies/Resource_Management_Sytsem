package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class AvailabilityExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public AvailabilityExceptionHandler(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static AvailabilityExceptionHandler notFound(String message) {
        return new AvailabilityExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static AvailabilityExceptionHandler badRequest(String message) {
        return new AvailabilityExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static AvailabilityExceptionHandler conflict(String message) {
        return new AvailabilityExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
