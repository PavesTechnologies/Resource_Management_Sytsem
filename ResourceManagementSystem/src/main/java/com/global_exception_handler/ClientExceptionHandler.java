package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class ClientExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public ClientExceptionHandler(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static ClientExceptionHandler notFound(String message) {
        return new ClientExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ClientExceptionHandler badRequest(String message) {
        return new ClientExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static ClientExceptionHandler conflict(String message) {
        return new ClientExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() { return status; }
    public String getErrorCode() { return errorCode; }
}
