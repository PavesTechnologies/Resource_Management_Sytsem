package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class CDCExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public CDCExceptionHandler(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static CDCExceptionHandler notFound(String message) {
        return new CDCExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static CDCExceptionHandler badRequest(String message) {
        return new CDCExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static CDCExceptionHandler conflict(String message) {
        return new CDCExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
