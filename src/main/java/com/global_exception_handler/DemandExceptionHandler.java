package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class DemandExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public DemandExceptionHandler(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static DemandExceptionHandler notFound(String message) {
        return new DemandExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static DemandExceptionHandler badRequest(String message) {
        return new DemandExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static DemandExceptionHandler conflict(String message) {
        return new DemandExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
