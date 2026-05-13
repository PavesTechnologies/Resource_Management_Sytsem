package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class RoleOffExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public RoleOffExceptionHandler(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static RoleOffExceptionHandler notFound(String message) {
        return new RoleOffExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static RoleOffExceptionHandler badRequest(String message) {
        return new RoleOffExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static RoleOffExceptionHandler conflict(String message) {
        return new RoleOffExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
