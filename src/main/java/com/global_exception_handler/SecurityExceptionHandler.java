package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class SecurityExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public SecurityExceptionHandler(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static SecurityExceptionHandler unauthorized(String message) {
        return new SecurityExceptionHandler(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static SecurityExceptionHandler forbidden(String message) {
        return new SecurityExceptionHandler(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static SecurityExceptionHandler badRequest(String message) {
        return new SecurityExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
