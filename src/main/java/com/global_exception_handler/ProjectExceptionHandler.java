package com.global_exception_handler;

import org.springframework.http.HttpStatus;

public class ProjectExceptionHandler extends RuntimeException {
    private final HttpStatus status;
    private final String errorCode;

    public ProjectExceptionHandler(
            HttpStatus status,
            String errorCode,
            String message
    ) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static ProjectExceptionHandler notFound(String message) {
        return new ProjectExceptionHandler(HttpStatus.NOT_FOUND, "NOT_FOUND", message);
    }

    public static ProjectExceptionHandler badRequest(String message) {
        return new ProjectExceptionHandler(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static ProjectExceptionHandler conflict(String message) {
        return new ProjectExceptionHandler(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
