package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class AllocationExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public AllocationExceptionHandler(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static AllocationExceptionHandler notFound(String message) {
        return new AllocationExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static AllocationExceptionHandler badRequest(String message) {
        return new AllocationExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static AllocationExceptionHandler conflict(String message) {
        return new AllocationExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
